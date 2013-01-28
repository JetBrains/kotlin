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

package org.jetbrains.k2js.test.config;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;

import java.util.List;

public class TestConfig extends Config {

    @NotNull
    public static TestConfigFactory FACTORY = new TestConfigFactory() {
        @Override
        public TestConfig create(@NotNull Project project,
                @NotNull EcmaVersion version,
                @NotNull List<JetFile> files,
                @NotNull BindingContext context) {
            return new TestConfig(project, version, files, context);
        }
    };

    @NotNull
    private final List<JetFile> jsLibFiles;
    @NotNull
    private final BindingContext libraryContext;

    public TestConfig(@NotNull Project project, @NotNull EcmaVersion version,
            @NotNull List<JetFile> files, @NotNull BindingContext context) {
        super(project, REWRITABLE_MODULE_NAME, version);
        jsLibFiles = files;
        libraryContext = context;
    }

    @Override
    public BindingContext getLibraryBindingContext() {
        return libraryContext;
    }

    @Override
    @NotNull
    public List<JetFile> generateLibFiles() {
        return jsLibFiles;
    }
}
