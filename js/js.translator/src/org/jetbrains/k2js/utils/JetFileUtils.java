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

package org.jetbrains.k2js.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class JetFileUtils {

    private JetFileUtils() {
    }

    @NotNull
    private static String loadFile(@NotNull String path) throws IOException {
        return doLoadFile(path);
    }

    @NotNull
    private static String doLoadFile(@NotNull String path) throws IOException {
        String text = FileUtil.loadFile(new File(path), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        return text;
    }

    @NotNull
    public static JetFile createPsiFile(@NotNull String name,
                                        @NotNull String text,
                                        @NotNull Project project) {
        String fileName = name.endsWith(".kt") ? name : name + ".jet";
        return (JetFile) createFile(fileName, text, project);
    }

    @NotNull
    private static JetFile loadPsiFile(@NotNull String name, @NotNull Project project) {
        try {
            return createPsiFile(name, loadFile(name), project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static PsiFile createFile(@NotNull String name, @NotNull String text, @NotNull Project project) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        PsiFile result = ((PsiFileFactoryImpl) PsiFileFactory.getInstance(project))
                .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
        assert result != null;
        return result;
    }


    @NotNull
    public static List<JetFile> createPsiFileList(@NotNull List<String> inputFiles,
                                                  @NotNull Project project) {
        List<JetFile> psiFiles = new ArrayList<JetFile>();
        for (String inputFile : inputFiles) {
            psiFiles.add(loadPsiFile(inputFile, project));
        }
        return psiFiles;
    }
}
