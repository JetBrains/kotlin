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
public final class IDEAConfig extends Config {

    @NotNull
    private final String pathToLibZip;

    public IDEAConfig(@NotNull Project project, @NotNull String pathToLibZip) {
        super(project);
        this.pathToLibZip = pathToLibZip;
    }

    public IDEAConfig(@NotNull Project project) {
        //TODO: testing purposes. Should not get anywhere near production
        this(project, "C:\\Dev\\Projects\\Kotlin\\clean_jet\\js\\js.libraries\\src\\k2jslib.zip");
    }

    @NotNull
    @Override
    public List<JetFile> getLibFiles() {
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
            return Collections.emptyList();
        }
    }

    @NotNull
    private List<JetFile> traverseArchive(@NotNull ZipFile file) throws IOException {
        List<JetFile> result = Lists.newArrayList();
        Enumeration<? extends ZipEntry> zipEntries = file.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            if (!entry.isDirectory()) {
                InputStream stream = file.getInputStream(entry);
                String text = FileUtil.loadTextAndClose(stream);
                JetFile jetFile = JetFileUtils.createPsiFile(entry.getName(), text, getProject());
                result.add(jetFile);
            }
        }
        return result;
    }
}
