/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * A helper class to discover a META-INF/services file on the classpath and load the files referenced inside it
 */
public class MetaInfServices {
    public static List<JetFile> loadServicesFiles(String metaInfServicesFile, Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        Set<URL> urlsLoaded = new HashSet<URL>();
        try {
            Enumeration<URL> resources = MetaInfServices.class.getClassLoader().getResources(metaInfServicesFile);
            loadLibFiles(resources, urlsLoaded, libFiles, project);
            resources = Thread.currentThread().getContextClassLoader().getResources(metaInfServicesFile);
            loadLibFiles(resources, urlsLoaded, libFiles, project);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return libFiles;
    }

    protected static void loadLibFiles(Enumeration<URL> resources, Set<URL> urlsLoaded, List<JetFile> libFiles, Project project) throws IOException {
        while (resources != null && resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if (url != null) {
                if (urlsLoaded.add(url)) {
                    System.out.println("Loading Kotlin JS library file: " + url);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                    try {
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            } else {
                                line = line.trim();
                                if (line.length() == 0 || line.startsWith("#")) continue;
                                // lets try to discover the file
                                InputStream stream = loadClasspathResource(line);
                                if (stream == null) {
                                    System.out.println("WARNING: failed to find JS source file: " + line + " on the classpath");
                                } else {
                                    //System.out.println("Loading JS library file: " + line);
                                    String text = FileUtil.loadTextAndClose(stream);
                                    JetFile file = JetFileUtils.createPsiFile(line, text, project);
                                    if (file != null) {
                                        //System.out.println("Parsing file: " + text);
                                        libFiles.add(file);
                                    }
                                }
                            }
                        }
                    } finally {
                        reader.close();
                    }
                }
            }
        }

    }

    /**
     * Tries to load the given resource name on the classpath
     */
    public static InputStream loadClasspathResource(String resourceName) {
        InputStream answer = ClassPathLibraryDefintionsConfig.class.getClassLoader().getResourceAsStream(resourceName);
        if (answer == null) {
            answer = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        }
        return answer;
    }
}
