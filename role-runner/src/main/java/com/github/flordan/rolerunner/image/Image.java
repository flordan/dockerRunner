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
package com.github.flordan.rolerunner.image;

import com.github.flordan.rolerunner.container.Container;
import com.github.flordan.rolerunner.container.ContainerManager;
import com.github.flordan.rolerunner.exception.ImageNotFoundException;

import java.util.HashSet;
import java.util.Set;

public abstract class Image {

    private final ImageManager monitor;
    private final Set<ImageIdentifier> tags;
    private final Set<Container> containers;

    public Image() {
        this(null);
    }

    public Image(ImageManager monitor) {
        this.monitor = monitor;
        tags = new HashSet<>();
        containers = new HashSet<>();
    }

    public void addTag(ImageIdentifier iId) {
        this.tags.add(iId);
    }

    public void removeTag(ImageIdentifier iId) {
        this.tags.remove(iId);
    }

    public Set<ImageIdentifier> getTags() {
        return this.tags;
    }

    public void addContainer(Container dc) {
        this.containers.add(dc);
    }

    public void removeContainer(Container dc) {
        this.containers.remove(dc);
    }

    public Set<Container> getContainers() {
        return this.containers;
    }

    public final void fetched() {
        if (this.monitor != null) {
            this.monitor.fetchedImage(this);
        }
    }

    public abstract void delete();

    public final void deleted() {
        if (this.monitor != null) {
            this.monitor.deletedImage(this);
        }
    }

    public abstract void createContainer(ContainerManager monitor) throws ImageNotFoundException;
}
