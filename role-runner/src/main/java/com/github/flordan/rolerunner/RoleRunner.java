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
import com.github.flordan.rolerunner.image.Image;
import com.github.flordan.rolerunner.image.ImageIdentifier;

import java.util.Set;


public interface RoleRunner<I extends Image, C extends Container> {

    boolean isImageAvailable(ImageIdentifier tag);

    Set<ImageIdentifier> getAvailableImages();

    void fetchImage(ImageIdentifier iId);

    void startRole(ImageIdentifier iId);
}
