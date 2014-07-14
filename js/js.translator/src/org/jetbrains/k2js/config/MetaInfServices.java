/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

/**
 * A helper class to discover a META-INF/services file on the classpath and load the files referenced inside it
 */
public final class MetaInfServices {
    private MetaInfServices() {
    }

    public static List<JetFile> loadServicesFiles(@NotNull String metaInfServicesFile, @NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        Set<URL> urlsLoaded = new HashSet<URL>();
        try {
            Enumeration<URL> resources = MetaInfServices.class.getClassLoader().getResources(metaInfServicesFile);
            loadLibFiles(resources, urlsLoaded, libFiles, project);
            resources = Thread.currentThread().getContextClassLoader().getResources(metaInfServicesFile);
            loadLibFiles(resources, urlsLoaded, libFiles, project);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return libFiles;
    }

    private static void loadLibFiles(@NotNull Enumeration<URL> resources,
            @NotNull Set<URL> urlsLoaded,
            @NotNull List<JetFile> libFiles,
            @NotNull Project project)
            throws IOException {
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            if (url != null) {
                if (urlsLoaded.add(url)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                    try {
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            else {
                                line = line.trim();
                                if (line.length() == 0 || line.startsWith("#")) continue;
                                // lets try to discover the file
                                InputStream stream = loadClasspathResource(line);
                                if (stream != null) {
                                    String text = StringUtil.convertLineSeparators(FileUtil.loadTextAndClose(stream));
                                    libFiles.add(JetPsiFactory(project).createFile(line, text));
                                }
                            }
                        }
                    }
                    finally {
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
