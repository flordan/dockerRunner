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

import com.github.flordan.rolerunner.container.Container;
import com.github.flordan.rolerunner.container.ContainerManager;

public class DockerContainer extends Container {
    private final String id;
    private final String name;
    private final DockerImage image;
    private final ContainerManager<DockerContainer, DockerImage> handler;

    private static enum Status {
        CREATED,
        RUNNING,
        STOPPED,
        DESTROYED
    }

    private Status state;

    public DockerContainer(String id, String name, DockerImage image) {
        this(id, name, image, null);
    }

    public DockerContainer(String id, String name, DockerImage image,
        ContainerManager<DockerContainer, DockerImage> handler) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.state = Status.CREATED;
        this.handler = handler;
        if (handler != null) {
            handler.createdContainer(this);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DockerImage getImage() {
        return image;
    }


    public void started() {
        System.out.println("Container " + this.id + " has started");
        this.state = Status.RUNNING;
    }

    public void stopped() {
        System.out.println("Container " + this.id + " has stopped");
        this.state = Status.STOPPED;
    }

    public void destroyed() {
        this.state = Status.DESTROYED;
        this.image.removeContainer(this);
        if (handler != null) {
            handler.destroyedContainer(this);
        }
    }

    public String getStatus() {
        return this.state.toString();
    }
}
