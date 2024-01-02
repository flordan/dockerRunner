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

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class ImageManager<I extends Image> {

    public interface ImageHandler<I> {
        public void requestImage(ImageIdentifier iId, ImageManager handler);

        I getImage(ImageIdentifier iId);
    }

    public interface ObtainCallback<I> {
        public void obtained(I img);
    }

    private final ImageHandler<I> handler;
    private final TreeMap<ImageIdentifier, List<ObtainCallback<I>>> pendingRequests;
    private final List<I> images;

    public ImageManager(ImageHandler<I> handler) {
        this.handler = handler;
        images = new LinkedList<>();
        pendingRequests = new TreeMap<>();
    }

    public boolean isImageAvailable(ImageIdentifier iId) {
        return handler.getImage(iId) != null;
    }

    public final synchronized void obtainImage(ImageIdentifier iId, ObtainCallback<I> callback) {
        System.out.println("IM obtaining " + iId);
        I img = handler.getImage(iId);
        if (img != null) {
            System.out.println("\tAlready Present");
            callback.obtained(img);
        } else {
            if (callback!=null) {
                synchronized (pendingRequests) {
                    System.out.println("\tRequesting");
                    List<ObtainCallback<I>> cbs = pendingRequests.get(iId);
                    if (cbs == null) {
                        cbs = new LinkedList<>();
                        pendingRequests.put(iId, cbs);
                    }
                    cbs.add(callback);
                }
            }
            handler.requestImage(iId, this);
        }
    }

    public final void fetchedImage(I img) {
        System.out.println("Obtained " + img.getTags());
        images.add(img);
        synchronized (pendingRequests) {
            for (ImageIdentifier iId : img.getTags()) {
                List<ObtainCallback<I>> cbs = this.pendingRequests.remove(iId);
                if (cbs != null) {
                    for (ObtainCallback cb : cbs) {
                        cb.obtained(img);
                    }
                }
            }
        }
    }

    public final void deletedImage(I img) {
        images.remove(img);
    }

    public final void clear() {
        List<I> toDelete = new LinkedList<>();
        for (I img : images) {
            toDelete.add(img);
        }
        for (I img : toDelete) {
            img.delete();
        }
    }
}
