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

package org.jetbrains.k2js.test.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Talanov
 */
//TODO: review/refactor
public final class TestConfig extends Config {


    @Nullable
    private /*var*/ List<JetFile> jsLibFiles = null;

    public TestConfig(@NotNull Project project) {
        super(project);
    }

    @NotNull
    private static List<JetFile> initLibFiles(@NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            JetFile file = null;
            //TODO: close stream?
            try {
                @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
                InputStream stream = new FileInputStream(LIBRARIES_LOCATION + libFileName);
                try {
                    String text = FileUtil.loadTextAndClose(stream);
                    file = JetFileUtils.createPsiFile(libFileName, text, project);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                libFiles.add(file);
            } catch (FileNotFoundException e) {
                //TODO: throw generic expception
                throw new IllegalStateException(e);
            }

        }
        return libFiles;
    }

    @NotNull
    public List<JetFile> getLibFiles() {
        if (jsLibFiles == null) {
            jsLibFiles = initLibFiles(getProject());
        }
        return jsLibFiles;
    }
}
