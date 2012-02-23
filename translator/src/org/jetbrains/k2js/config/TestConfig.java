/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import core.Dummy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
//TODO: review/refactor
public final class TestConfig extends Config {

    //TODO: provide some generic way to get the files of the project
    @NotNull
    private static final List<String> LIB_FILE_NAMES = Arrays.asList(
            "/core/annotations.kt",
            "/jquery/common.kt",
            "/jquery/ui.kt",
            "/core/javautil.kt",
            "/core/javalang.kt",
            "/core/core.kt",
            "/core/math.kt",
            "/core/json.kt",
            "/raphael/raphael.kt",
            "/html5/canvas.kt",
            "/html5/files.kt",
            "/html5/image.kt",
            "/helper/ip.kt"
    );

    @NotNull
    private static JetCoreEnvironment getTestEnvironment() {
        if (testOnlyEnvironment == null) {
            testOnlyEnvironment = new JetCoreEnvironment(new Disposable() {
                @Override
                public void dispose() {
                }
            });
        }
        return testOnlyEnvironment;
    }

    @Nullable
    private static /*var*/ JetCoreEnvironment testOnlyEnvironment = null;


    @Nullable
    private /*var*/ List<JetFile> jsLibFiles = null;

    public TestConfig() {
    }

    @NotNull
    @Override
    public Project getProject() {
        return getTestEnvironment().getProject();
    }

    @NotNull
    private static List<JetFile> initLibFiles(@NotNull Project project) {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            JetFile file = null;
            //TODO: close stream?
            InputStream stream = Dummy.class.getResourceAsStream(libFileName);
            try {
                String text = readString(stream);
                file = JetFileUtils.createPsiFile(libFileName, text, project);
            } catch (IOException e) {
                e.printStackTrace();
            }
            libFiles.add(file);
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

    @NotNull
    private static String readString(@NotNull InputStream is) throws IOException {
        char[] buf = new char[2048];
        Reader r = new InputStreamReader(is, "UTF-8");
        StringBuilder s = new StringBuilder();
        while (true) {
            int n = r.read(buf);
            if (n < 0)
                break;
            s.append(buf, 0, n);
        }
        return s.toString();
    }
}
