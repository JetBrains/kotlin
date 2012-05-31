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
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Pavel Talanov
 */
public class ZippedLibrarySourcesConfig extends Config {
    @Nullable
    protected final String pathToLibZip;

    public ZippedLibrarySourcesConfig(@NotNull Project project, @Nullable String pathToZip, @NotNull EcmaVersion ecmaVersion) {
        super(project, ecmaVersion);
        pathToLibZip = pathToZip;
    }

    @NotNull
    @Override
    public List<JetFile> generateLibFiles() {
        System.out.println("Parsing JS library source zip: " + pathToLibZip);
        if (pathToLibZip == null) {
            return Collections.emptyList();
        }
        try {
            File file = new File(pathToLibZip);
            ZipFile zipFile = new ZipFile(file);
            try {
                return traverseArchive(zipFile);
            }
            finally {
                zipFile.close();
            }
        }
        catch (IOException e) {
            System.out.println("Failed to process " + pathToLibZip + ". Reason: " + e);
            e.printStackTrace();
            return Collections.emptyList();
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
                System.out.println("Parsing file: " + entry.getName());
                result.add(jetFile);
            }
        }
        return result;
    }
}
