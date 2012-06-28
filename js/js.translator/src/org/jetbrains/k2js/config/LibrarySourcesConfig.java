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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Pavel Talanov
 */
public class LibrarySourcesConfig extends Config {
    public static final Key<String> EXTERNAL_MODULE_NAME = new Key<String>("externalModule");
    public static final String UNKNOWN_EXTERNAL_MODULE_NAME = "<unknown>";

    @Nullable
    private final String[] files;

    public LibrarySourcesConfig(@NotNull Project project,
            @NotNull String moduleId,
            @Nullable String[] files,
            @NotNull EcmaVersion ecmaVersion) {
        super(project, moduleId, ecmaVersion);
        this.files = files;
    }

    @NotNull
    @Override
    public List<JetFile> generateLibFiles() {
        if (files == null) {
            return Collections.emptyList();
        }

        List<JetFile> jetFiles = new ArrayList<JetFile>();
        String moduleName = UNKNOWN_EXTERNAL_MODULE_NAME;
        for (String path : files) {
            File file = new File(path);
            try {
                String name = file.getName();
                if (path.charAt(0) == '@') {
                    moduleName = path.substring(1);
                    continue;
                }

                if (name.endsWith(".jar") || name.endsWith(".zip")) {
                    jetFiles.addAll(readZip(file));
                }
                else {
                    JetFile psiFile = JetFileUtils.createPsiFile(path, FileUtil.loadFile(file), getProject());
                    psiFile.putUserData(EXTERNAL_MODULE_NAME, moduleName);
                    jetFiles.add(psiFile);
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return jetFiles;
    }

    private List<JetFile> readZip(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        try {
            return traverseArchive(zipFile);
        }
        finally {
            zipFile.close();
        }
    }

    @NotNull
    private List<JetFile> traverseArchive(@NotNull ZipFile file) throws IOException {
        List<JetFile> result = Lists.newArrayList();
        Enumeration<? extends ZipEntry> zipEntries = file.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".kt")) {
                InputStream stream = file.getInputStream(entry);
                String text = FileUtil.loadTextAndClose(stream);
                JetFile jetFile = JetFileUtils.createPsiFile(entry.getName(), text, getProject());
                jetFile.putUserData(EXTERNAL_MODULE_NAME, UNKNOWN_EXTERNAL_MODULE_NAME);
                result.add(jetFile);
            }
        }
        return result;
    }
}
