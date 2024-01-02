/*
 *  Copyright 2023 Francesc Lordan Gomis
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.github.flordan.rolerunner.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.flordan.rolerunner.container.ContainerManager;
import com.github.flordan.rolerunner.exception.ImageNotFoundException;
import com.github.flordan.rolerunner.image.ImageIdentifier;
import com.github.flordan.rolerunner.image.ImageManager;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DockerManager {

    private final DockerClient client;
    private final Map<String, DockerImage> images;
    private final Map<ImageIdentifier, DockerImage> tags;
    private final Map<ImageIdentifier, ImageManager> requestedTags;
    private final Map<String, ContainerManager<DockerContainer, DockerImage>> requestedContainers;
    private final Map<String, DockerContainer> containers;

    public DockerManager() {


        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

        this.client = DockerClientImpl.getInstance(config, httpClient);

        images = new TreeMap<>();
        tags = new TreeMap<>();
        requestedTags = new TreeMap<>();
        containers = new TreeMap<>();
        requestedContainers = new TreeMap<>();
        this.client.eventsCmd().exec(new DockerMonitor());
        loadCurrentState();

    }

    private void loadCurrentState() {
        List<Image> images = this.client.listImagesCmd().exec();
        for (Image i : images) {
            String id = i.getId();
            DockerImage di = new DockerImage(id);
            for (String tag : i.getRepoTags()) {
                ImageIdentifier iId = ImageIdentifier.parse(tag);
                di.addTag(iId);
                tags.put(iId, di);
            }
            this.images.put(id, di);
        }
        List<Container> containers = this.client.listContainersCmd().exec();
        for (Container c : containers) {
            String containerID = c.getId();
            String name = c.getNames()[0];
            String imageID = c.getImageId();
            DockerImage di = this.images.get(imageID);
            DockerContainer dc = new DockerContainer(containerID, name, di);
            switch (c.getState()) {
                case "running":
                    dc.started();
                    break;
                case "exited":
                    dc.stopped();
                    break;
                default:
                    // Assume Created
            }
            this.containers.put(containerID, dc);
            di.addContainer(dc);
        }
    }

    private void printCurrentState() {
        for (DockerImage i : this.images.values()) {
            System.out.println(i.getID());
            System.out.println("├─tags:");
            Iterator<ImageIdentifier> tags = i.getTags().iterator();
            while (tags.hasNext()) {
                ImageIdentifier tag = tags.next();
                if (tags.hasNext()) {
                    System.out.println("│    ├─" + tag);
                } else {
                    System.out.println("│    └─" + tag);
                }
            }
            System.out.println("└─containers:");
            Iterator<DockerContainer> containers = i.getContainers().iterator();
            while (containers.hasNext()) {
                DockerContainer container = containers.next();
                if (containers.hasNext()) {
                    System.out.println("     ├─" + container.getName() + " - " + container.getId());
                } else {
                    System.out.println("     └─" + container.getName() + " - " + container.getId());
                }
            }
        }

        for (Map.Entry<ImageIdentifier, DockerImage> tagEntry : tags.entrySet()) {
            System.out.println(tagEntry.getKey() + "-->" + tagEntry.getValue().getID());
        }
    }

    public Set<ImageIdentifier> getAvailableImages() {
        return this.tags.keySet();
    }

    public DockerImage getImage(ImageIdentifier iId) {
        return this.tags.get(iId);
    }

    public void requestImage(ImageIdentifier iId, ImageManager handler) {
        synchronized (requestedTags) {
            requestedTags.put(iId, handler);
        }
        PullImageResultCallback cb = new PullCallback();
        client.pullImageCmd(iId.getRepository()).withTag(iId.getTag()).exec(cb);
    }

    private static class PullCallback extends PullImageResultCallback {
        public void onNext(PullResponseItem item) {
        }
    }

    public void deleteImage(DockerImage image) {
        client.removeImageCmd(image.getID()).exec();
    }


    public void createContainer(DockerImage image, ContainerManager<DockerContainer, DockerImage> handler)
        throws ImageNotFoundException {
        System.out.println("Create container for image " + image.getID() + " " + image.getTags());
        HostConfig hostConfig = HostConfig
            .newHostConfig()
            .withBinds(new Bind("colmena", new Volume("/colmena")))
            .withAutoRemove(true);

        try {
            synchronized (this.requestedContainers) {
                CreateContainerResponse response = client.createContainerCmd(image.getID())
                    .withHostConfig(hostConfig)
                    .withCmd("sleep", "1000")
                    .exec();
                this.requestedContainers.put(response.getId(), handler);
            }
        } catch (NotFoundException notFoundException) {
            throw new ImageNotFoundException();
        }
    }


    public void startContainer(DockerContainer cnt) {
        client.startContainerCmd(cnt.getId()).exec();
    }

    public void destroyContainer(DockerContainer cnt) {
        client.removeContainerCmd(cnt.getId()).exec();
    }

    private class DockerMonitor extends ResultCallback.Adapter<Event> {

        public DockerMonitor() {
        }

        public void onNext(Event event) {
            switch (event.getType()) {
                case CONTAINER:
                    containerEvent(event);
                    break;
                case IMAGE:
                    imageEvent(event);
                    break;
            }
        }

        private void containerEvent(Event event) {
            switch (event.getAction()) {
                case "create":
                    createdContainer(event);
                    break;
                case "start":
                    startedContainer(event);
                    break;
                case "die":
                    deadContainer(event);
                    break;
                case "destroy":
                    destroyedContainer(event);
                    break;
                default:
                    // Ignore Event
            }
        }

        private void createdContainer(Event event) {
            try {
                String id = event.getId();
                String name = event.getActor().getAttributes().get("name");
                String imageId = event.getActor().getAttributes().get("image");
                DockerImage di = images.get(imageId);
                ContainerManager handler;
                synchronized (requestedContainers) {
                    handler = requestedContainers.remove(id);
                }
                DockerContainer dc = new DockerContainer(id, name, di, handler);
                containers.put(id, dc);
                di.addContainer(dc);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

        private void startedContainer(Event event) {
            String id = event.getId();
            DockerContainer dc = containers.get(id);
            dc.started();
        }

        private void deadContainer(Event event) {
            String id = event.getId();
            DockerContainer dc = containers.get(id);
            dc.stopped();
        }

        private void destroyedContainer(Event event) {
            String id = event.getId();
            DockerContainer dc = containers.remove(id);
            dc.destroyed();
        }

        private void imageEvent(Event event) {
            switch (event.getAction()) {
                case "pull":
                    pulledImage(event);
                    break;
                case "tag":
                    taggedImage(event);
                    break;
                case "delete":
                    deletedImage(event);
                    break;
                default:
                    // Ignore Event
            }
        }


        private void deletedImage(Event event) {
            String deletedId = event.getId();
            DockerImage img = images.remove(deletedId);
            for (ImageIdentifier tag : img.getTags()) {
                tags.remove(tag);
            }
            img.deleted();
        }

        private void pulledImage(Event event) {
            String pulledTag = event.getId();
            InspectImageResponse response = DockerManager.this.client.inspectImageCmd(pulledTag).exec();
            String imageId = response.getId();
            ImageIdentifier pulledIId = ImageIdentifier.parse(pulledTag);
            ImageManager handler;
            synchronized (requestedTags) {
                handler = requestedTags.remove(pulledIId);
            }
            DockerImage image = new DockerImage(imageId, handler);
            images.put(imageId, image);
            image.addTag(pulledIId);
            tags.put(pulledIId, image);
            image.fetched();
        }

        private void taggedImage(Event event) {
            String taggedId = event.getId();
            String tag = event.getActor().getAttributes().get("name");
            ImageIdentifier iId = ImageIdentifier.parse(tag);
            DockerImage oldImage = tags.get(iId);
            if (oldImage != null) {
                oldImage.removeTag(iId);
            }
            DockerImage im = images.get(taggedId);
            if (im == null) {
                im = new DockerImage(taggedId);
                images.put(taggedId, im);
            }

            im.addTag(iId);
            tags.put(iId, im);
        }

    }
}