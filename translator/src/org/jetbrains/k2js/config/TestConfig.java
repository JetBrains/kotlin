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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.JetCoreEnvironment;

/**
 * @author Pavel Talanov
 */
public final class TestConfig extends Config {

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

    public TestConfig() {
    }

    @NotNull
    @Override
    public Project getProject() {
        return getTestEnvironment().getProject();
    }
}
