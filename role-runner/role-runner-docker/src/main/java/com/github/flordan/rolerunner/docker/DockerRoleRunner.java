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

import com.github.flordan.rolerunner.RoleRunnerImpl;
import com.github.flordan.rolerunner.container.ContainerManager;
import com.github.flordan.rolerunner.exception.ImageNotFoundException;
import com.github.flordan.rolerunner.image.ImageIdentifier;
import com.github.flordan.rolerunner.image.ImageManager;

import java.util.Set;

public class DockerRoleRunner extends RoleRunnerImpl<DockerImage, DockerContainer> {

    private final DockerMonitor monitor = new DockerMonitor();

    @Override
    public Set<ImageIdentifier> getAvailableImages() {
        return monitor.getAvailableImages();
    }

    @Override
    public DockerImage getImage(ImageIdentifier iId) {
        return monitor.getImage(iId);
    }

    @Override
    public void requestImage(ImageIdentifier iId, ImageManager handler) {
        monitor.requestImage(iId, handler);
    }

    @Override
    public void deleteImage(DockerImage img) {
        monitor.deleteImage(img);
    }

    @Override
    public void createContainer(DockerImage img, ContainerManager<DockerContainer, DockerImage> handler) throws ImageNotFoundException {
        monitor.createContainer(img, handler);
    }

    @Override
    public void startContainer(DockerContainer cnt) {
        monitor.startContainer(cnt);
    }

    @Override
    public void destroyContainer(DockerContainer cnt) {

        monitor.destroyContainer(cnt);
    }
}
