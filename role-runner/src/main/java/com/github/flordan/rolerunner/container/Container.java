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
package com.github.flordan.rolerunner.container;

import com.github.flordan.rolerunner.image.Image;

public abstract class Container {


    private static enum Status {
        PENDING,
        CREATED,
        RUNNING,
        STOPPED,
        DESTROYED
    }

    private final ContainerManager monitor;
    private final Image image;
    private Status state = Status.PENDING;

    public Container(Image image) {
        this(image, null);
    }

    public Container(Image image, ContainerManager monitor) {
        this.image = image;
        this.monitor = monitor;
    }

    public Image getImage() {
        return image;
    }

    public final String getStatus() {
        return this.state.toString();
    }

    public void created() {
        System.out.println("Container " + this + " has been created");
        this.state = Status.CREATED;
        if (monitor != null) {
            monitor.createdContainer(this);
        }
    }

    public abstract void start();

    public void started() {
        System.out.println("Container " + this + " has started");
        this.state = Status.RUNNING;
    }

    public void stopped() {
        System.out.println("Container " + this + " has stopped");
        this.state = Status.STOPPED;
    }

    public abstract void destroy();

    public void destroyed() {
        this.state = Status.DESTROYED;
        this.image.removeContainer(this);
        if (monitor != null) {
            monitor.destroyedContainer(this);
        }
    }
}
