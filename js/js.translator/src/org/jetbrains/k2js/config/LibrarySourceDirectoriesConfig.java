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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LibrarySourceDirectoriesConfig extends Config {
    @NotNull
    protected final String[] directories;

    public LibrarySourceDirectoriesConfig(@NotNull Project project, @NotNull String moduleId, @NotNull String[] directories, @NotNull EcmaVersion ecmaVersion) {
        super(project, moduleId, ecmaVersion);
        this.directories = directories;
    }

    @NotNull
    @Override
    public List<JetFile> generateLibFiles() {
        try {
            List<JetFile> results = Lists.newArrayList();
            for (String directory : directories) {
                File rootDir = new File(directory);
                results.addAll(traverseDirectory(rootDir, rootDir));
            }
            return results;
        }
        catch (IOException e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @NotNull
    private List<JetFile> traverseDirectory(@NotNull File rootDir, @NotNull File dir) throws IOException {
        File[] files = dir.listFiles();
        File[] children = dir.listFiles();
        List<JetFile> result = Lists.newArrayList();
        if (children != null && children.length > 0) {
            for (File child : children) {
                if (child.isDirectory()) {
                    List<JetFile> childFiles = traverseDirectory(rootDir, child);
                    result.addAll(childFiles);
                }
                else {
                    String name = child.getName();
                    if (name.toLowerCase().endsWith(".kt")) {
                        String text = FileUtil.loadFile(child);
                        //String path = FileUtil.getRelativePath(directoryFile, child);
                        String path = child.getPath();
                        JetFile jfile = JetFileUtils.createPsiFile(path, text, getProject());
                        result.add(jfile);
                    }
                }
            }
        }
        return result;
    }
}
