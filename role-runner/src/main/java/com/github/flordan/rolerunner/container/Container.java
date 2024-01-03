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

import java.util.LinkedList;
import java.util.Deque;

public abstract class Container {


    protected static enum Status {
        PENDING,
        CREATED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        DESTROYING,
        DESTROYED
    }

    private static enum Action {
        START,
        STOP,
        DESTROY
    }

    private final ContainerManager monitor;
    private final Image image;
    private Status state = Status.PENDING;
    private final Deque<Action> pendingActions;

    public Container(Image image) {
        this(image, null);
    }

    public Container(Image image, ContainerManager monitor) {
        this.image = image;
        this.monitor = monitor;
        pendingActions = new LinkedList<>();
    }

    public Image getImage() {
        return image;
    }

    public final Status getStatus() {
        return this.state;
    }

    public void created() {
        System.out.println("Container " + this + " has been created");
        this.state = Status.CREATED;
        if (monitor != null) {
            monitor.createdContainer(this);
        }
        manageLifecycle();
    }

    public final void start() {
        pendingActions.add(Action.START);
        manageLifecycle();
    }

    public abstract void specificStart();

    public void started() {
        System.out.println("Container " + this + " has started");
        this.state = Status.RUNNING;
        manageLifecycle();
    }

    public void stop() {
        pendingActions.add(Action.STOP);
        manageLifecycle();
    }

    public abstract void specificStop();

    public void stopped() {
        System.out.println("Container " + this + " has stopped");
        this.state = Status.STOPPED;
        manageLifecycle();
    }

    public void destroy() {
        pendingActions.add(Action.DESTROY);
        manageLifecycle();
    }

    public abstract void specificDestroy();

    public void destroyed() {
        this.state = Status.DESTROYED;
        this.image.removeContainer(this);
        if (monitor != null) {
            monitor.destroyedContainer(this);
        }
        manageLifecycle();
    }

    private void manageLifecycle() {
        if (pendingActions.isEmpty()) {
            return;
        }
        Action action;
        switch (state) {
            case CREATED:
                action = pendingActions.poll();
                switch (action) {
                    case START:
                        state = Status.STARTING;
                        specificStart();
                        break;
                    case STOP:
                        state = Status.STOPPED;
                        break;
                    case DESTROY:
                        state = Status.DESTROYING;
                        specificDestroy();
                        break;
                }
                break;
            case RUNNING:
                action = pendingActions.poll();
                switch (action) {
                    case START:
                        break;
                    case STOP:
                        state = Status.STOPPING;
                        specificStop();
                        break;
                    case DESTROY:
                        state = Status.STOPPING;
                        pendingActions.add(Action.DESTROY);
                        specificStop();
                        break;
                }
                break;
            case STOPPED:
                action = pendingActions.poll();
                switch (action) {
                    case START:
                        break;
                    case STOP:
                        break;
                    case DESTROY:
                        state = Status.DESTROYING;
                        specificDestroy();
                        break;
                }
                break;
            default:
                // wait until change
        }
    }
}
