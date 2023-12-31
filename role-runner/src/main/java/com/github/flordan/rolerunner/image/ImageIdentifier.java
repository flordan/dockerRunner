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

public class ImageIdentifier implements Comparable<ImageIdentifier> {
    private static final String DEFAULT_TAG = "latest";
    private final String registry;
    private final String repository;
    private final String tag;

    public static ImageIdentifier parse(String fullName) {
        String[] registrySplit = fullName.split("/");
        String registry = null;
        if (registrySplit.length > 1) {
            registry = registrySplit[0];
            fullName = registrySplit[1];
        }
        String[] tagSplit = fullName.split(":");
        String repository = tagSplit[0];
        String tag = DEFAULT_TAG;
        if (tagSplit.length > 1) {
            tag = tagSplit[1];
        }
        return new ImageIdentifier(registry, repository, tag);
    }

    public ImageIdentifier(String repository) {
        this(repository, DEFAULT_TAG);
    }

    public ImageIdentifier(String repository, String tag) {
        this(null, repository, tag);
    }

    public ImageIdentifier(String registry, String repository, String tag) {
        this.registry = registry;
        this.repository = repository;
        this.tag = tag;
    }

    public String getRegistry() {
        return registry;
    }

    public String getRepository() {
        return repository;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return (registry != null ? registry + "/" : "") + repository + ":" + tag;
    }

    @Override
    public int compareTo(ImageIdentifier o) {
        int val = this.repository.compareTo(o.repository);
        if (val==0){
            val = this.tag.compareTo(o.tag);
            if (val==0){
                if (this.registry !=null && !this.registry.isEmpty() && o.registry !=null && !o.registry.isEmpty()) {
                    val = this.registry.compareTo(o.registry);
                }
            }
        }
        return val;
    }
}
