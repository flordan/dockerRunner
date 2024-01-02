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

    private static final DockerClient CLIENT;
    private static final Map<String, DockerImage> IMAGES;
    private static final Map<ImageIdentifier, DockerImage> TAGS;
    private static final Map<String, DockerContainer> CONTAINERS;
    private static final Map<ImageIdentifier, ImageManager> REQ_TAGS;
    private static final Map<String, ContainerManager<DockerContainer, DockerImage>> REQ_CONTAINERS;

    static {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();
        CLIENT = DockerClientImpl.getInstance(config, httpClient);

        IMAGES = new TreeMap<>();
        TAGS = new TreeMap<>();
        CONTAINERS = new TreeMap<>();

        CLIENT.eventsCmd().exec(new DockerMonitor());
        loadCurrentState();

        REQ_TAGS = new TreeMap<>();
        REQ_CONTAINERS = new TreeMap<>();
    }

    private DockerManager() throws InstantiationException {
        throw new InstantiationException();
    }

    private static void loadCurrentState() {
        List<Image> images = CLIENT.listImagesCmd().exec();
        for (Image i : images) {
            String id = i.getId();
            DockerImage di = new DockerImage(id);
            for (String tag : i.getRepoTags()) {
                ImageIdentifier iId = ImageIdentifier.parse(tag);
                di.addTag(iId);
                TAGS.put(iId, di);
            }
            IMAGES.put(id, di);
        }
        List<Container> containers = CLIENT.listContainersCmd().exec();
        for (Container c : containers) {
            String containerID = c.getId();
            String name = c.getNames()[0];
            String imageID = c.getImageId();
            DockerImage di = IMAGES.get(imageID);
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
            CONTAINERS.put(containerID, dc);
            di.addContainer(dc);
        }
    }

    private static void printCurrentState() {
        for (DockerImage i : IMAGES.values()) {
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
            Iterator<com.github.flordan.rolerunner.container.Container> containers = i.getContainers().iterator();
            while (containers.hasNext()) {
                com.github.flordan.rolerunner.container.Container container = containers.next();
                if (containers.hasNext()) {
                    System.out.println("     ├─" + container);
                } else {
                    System.out.println("     └─" + container);
                }
            }
        }

        for (Map.Entry<ImageIdentifier, DockerImage> tagEntry : TAGS.entrySet()) {
            System.out.println(tagEntry.getKey() + "-->" + tagEntry.getValue().getID());
        }
    }

    public static Set<ImageIdentifier> getAvailableImages() {
        return TAGS.keySet();
    }

    public static DockerImage getImage(ImageIdentifier iId) {
        return TAGS.get(iId);
    }

    public static void requestImage(ImageIdentifier iId, ImageManager handler) {
        synchronized (REQ_TAGS) {
            REQ_TAGS.put(iId, handler);
        }
        PullImageResultCallback cb = new PullCallback();
        CLIENT.pullImageCmd(iId.getRepository()).withTag(iId.getTag()).exec(cb);
    }

    private static class PullCallback extends PullImageResultCallback {
        public void onNext(PullResponseItem item) {
        }
    }

    public static void deleteImage(DockerImage image) {
        CLIENT.removeImageCmd(image.getID()).exec();
    }


    public static void createContainer(DockerImage image, ContainerManager<DockerContainer, DockerImage> handler)
        throws ImageNotFoundException {
        System.out.println("Create container for image " + image.getID() + " " + image.getTags());
        HostConfig hostConfig = HostConfig
            .newHostConfig()
            .withBinds(new Bind("colmena", new Volume("/colmena")))
            .withAutoRemove(true);

        try {
            synchronized (REQ_CONTAINERS) {
                CreateContainerResponse response = CLIENT.createContainerCmd(image.getID())
                    .withHostConfig(hostConfig)
                    .withCmd("sleep", "1000")
                    .exec();
                REQ_CONTAINERS.put(response.getId(), handler);
            }
        } catch (NotFoundException notFoundException) {
            throw new ImageNotFoundException();
        }
    }


    public static void startContainer(DockerContainer cnt) {
        CLIENT.startContainerCmd(cnt.getId()).exec();
    }

    public static void destroyContainer(DockerContainer cnt) {
        CLIENT.removeContainerCmd(cnt.getId()).exec();
    }

    private static class DockerMonitor extends ResultCallback.Adapter<Event> {

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
                DockerImage di = IMAGES.get(imageId);
                ContainerManager handler;
                synchronized (REQ_CONTAINERS) {
                    handler = REQ_CONTAINERS.remove(id);
                }
                DockerContainer dc = new DockerContainer(id, name, di, handler);
                CONTAINERS.put(id, dc);
                di.addContainer(dc);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

        private void startedContainer(Event event) {
            String id = event.getId();
            DockerContainer dc = CONTAINERS.get(id);
            dc.started();
        }

        private void deadContainer(Event event) {
            String id = event.getId();
            DockerContainer dc = CONTAINERS.get(id);
            dc.stopped();
        }

        private void destroyedContainer(Event event) {
            String id = event.getId();
            DockerContainer dc = CONTAINERS.remove(id);
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
            DockerImage img = IMAGES.remove(deletedId);
            for (ImageIdentifier tag : img.getTags()) {
                TAGS.remove(tag);
            }
            img.deleted();
        }

        private void pulledImage(Event event) {
            String pulledTag = event.getId();
            InspectImageResponse response = DockerManager.CLIENT.inspectImageCmd(pulledTag).exec();
            String imageId = response.getId();
            ImageIdentifier pulledIId = ImageIdentifier.parse(pulledTag);
            ImageManager handler;
            synchronized (REQ_TAGS) {
                handler = REQ_TAGS.remove(pulledIId);
            }
            DockerImage image = new DockerImage(imageId, handler);
            IMAGES.put(imageId, image);
            image.addTag(pulledIId);
            TAGS.put(pulledIId, image);
            image.fetched();
        }

        private void taggedImage(Event event) {
            String taggedId = event.getId();
            String tag = event.getActor().getAttributes().get("name");
            ImageIdentifier iId = ImageIdentifier.parse(tag);
            DockerImage oldImage = TAGS.get(iId);
            if (oldImage != null) {
                oldImage.removeTag(iId);
            }
            DockerImage im = IMAGES.get(taggedId);
            if (im == null) {
                im = new DockerImage(taggedId);
                IMAGES.put(taggedId, im);
            }

            im.addTag(iId);
            TAGS.put(iId, im);
        }

    }
}