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
package nesteddocker;

import com.github.flordan.rolerunner.RoleRunner;
import com.github.flordan.rolerunner.docker.DockerRoleRunner;
import com.github.flordan.rolerunner.image.ImageIdentifier;


public class App {

    public static void main(String[] args) throws Exception {
        RoleRunner r = new DockerRoleRunner();

        ImageIdentifier iId= new ImageIdentifier("ubuntu", "latest");

        if (r.isImageAvailable(iId)){
            System.out.println("Role Ubuntu image available");
        }else{
            System.out.println("Role Ubuntu image not available");
            r.fetchImage(iId);
        }
        System.out.println(r.getAvailableImages());
        r.startRole(iId);



        ImageIdentifier alpineIId= new ImageIdentifier("alpine", "latest");
        r.startRole(alpineIId);
        Thread.sleep(3_000l);
        r.startRole(iId);

        synchronized(App.class) {
            App.class.wait();
        }
    }
}
