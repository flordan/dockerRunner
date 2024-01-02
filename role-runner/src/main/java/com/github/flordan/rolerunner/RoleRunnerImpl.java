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
package com.github.flordan.rolerunner;

import com.github.flordan.rolerunner.container.Container;
import com.github.flordan.rolerunner.container.ContainerManager;
import com.github.flordan.rolerunner.exception.ImageNotFoundException;
import com.github.flordan.rolerunner.image.Image;
import com.github.flordan.rolerunner.image.ImageIdentifier;
import com.github.flordan.rolerunner.image.ImageManager;

import java.util.Set;


public abstract class RoleRunnerImpl implements RoleRunner, ImageManager.ImageHandler {

    protected final ImageManager images;
    protected final ContainerManager containers;

    public RoleRunnerImpl() {
        this.images = new ImageManager(this);
        this.containers = new ContainerManager();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run () {
                containers.clear();
                images.clear();
            }
        });
    }

    public final boolean isImageAvailable(ImageIdentifier tag){
        return images.isImageAvailable(tag);
    }

    public abstract Set<ImageIdentifier> getAvailableImages();

    public final void fetchImage(ImageIdentifier iId){
        images.obtainImage(iId, null);
    }

    public final void startRole(ImageIdentifier iId) {
        System.out.println("Requesting role for image " + iId);
        images.obtainImage(iId, new ImageManager.ObtainCallback() {
            @Override
            public void obtained(Image img) {
                try {
                    img.createContainer(containers);
                } catch (ImageNotFoundException infe) {
                    startRole(iId);
                }
            }
        });
    }

}
