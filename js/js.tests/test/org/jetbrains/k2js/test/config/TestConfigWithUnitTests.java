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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.translate.test.JSRhinoUnitTester;
import org.jetbrains.k2js.translate.test.JSTester;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public class TestConfigWithUnitTests extends TestConfig {

    @NotNull
    public static TestConfigFactory FACTORY = new TestConfigFactory() {
        @Override
        public TestConfig create(@NotNull Project project,
                @NotNull EcmaVersion version,
                @NotNull List<JetFile> files,
                @NotNull BindingContext context) {
            return new TestConfigWithUnitTests(project, version, files, context);
        }
    };

    @Override
    public JSTester getTester() {
        return new JSRhinoUnitTester();
    }

    public TestConfigWithUnitTests(@NotNull Project project,
            @NotNull EcmaVersion version,
            @NotNull List<JetFile> files,
            @NotNull BindingContext context) {
        super(project, version, files, context);
    }
}
