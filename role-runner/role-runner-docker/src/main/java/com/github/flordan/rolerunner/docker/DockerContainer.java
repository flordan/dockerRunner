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

    public DockerContainer(String id, String name, DockerImage image) {
        this(id, name, image, null);
    }

    public DockerContainer(String id, String name, DockerImage image, ContainerManager handler) {
        super(image, handler);
        this.id = id;
        this.name = name;
        created();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public void specificStart() {
        DockerManager.startContainer(this);
    }

    @Override
    public void specificStop() {
        DockerManager.stopContainer(this);
    }

    public void specificDestroy() {
        DockerManager.destroyContainer(this);
    }
}
