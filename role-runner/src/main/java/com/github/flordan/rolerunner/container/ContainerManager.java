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

import com.github.flordan.rolerunner.exception.ImageNotFoundException;
import com.github.flordan.rolerunner.image.Image;
import com.github.flordan.rolerunner.image.ImageIdentifier;
import com.github.flordan.rolerunner.image.ImageManager;

import java.util.LinkedList;
import java.util.List;

public class ContainerManager<I extends Image> {


    public interface ContainerHandler<I extends Image> {
        void createContainer(I image, ContainerManager<I> manager) throws ImageNotFoundException;

    }

    private final ContainerHandler<I> handler;
    private final List<Container> containers;
    public ContainerManager(ContainerHandler<I> handler) {
        this.handler = handler;
        this.containers = new LinkedList<>();
    }

    public void startRole(I image) throws ImageNotFoundException {
        handler.createContainer(image, this);
    }


    public void createdContainer(Container cntr) {
        containers.add(cntr);
        cntr.start();
    }

    public void destroyedContainer(Container cntr) {
        containers.remove(cntr);
        synchronized (this) {
            this.notify();
        }
    }

    public final void clear() {
        List<Container> toDelete = new LinkedList<>();
        for (Container cntr : containers) {
            toDelete.add(cntr);
        }
        for (Container cntr : toDelete) {
            cntr.destroy();
        }
        System.out.println("Waiting for all containers to be removed.");
        synchronized (this) {
            while (!containers.isEmpty()) {
                try {
                    this.wait();
                }catch(InterruptedException ie){
                    // Do nothing
                }
            }
        }
    }
}
