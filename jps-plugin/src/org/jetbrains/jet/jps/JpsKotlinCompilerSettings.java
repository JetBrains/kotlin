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

package org.jetbrains.jet.jps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

public class JpsKotlinCompilerSettings extends JpsElementBase<JpsKotlinCompilerSettings> {
    static final JpsElementChildRole<JpsKotlinCompilerSettings> ROLE = JpsElementChildRoleBase.create("Kotlin Compiler Settings");

    @NotNull
    public CommonCompilerArguments commonCompilerSettings = new CommonCompilerArguments.DummyImpl();
    @NotNull
    public K2JVMCompilerArguments k2JvmCompilerSettings = new K2JVMCompilerArguments();
    @NotNull
    public K2JSCompilerArguments k2JsCompilerSettings = new K2JSCompilerArguments();

    @NotNull
    @Override
    public JpsKotlinCompilerSettings createCopy() {
        JpsKotlinCompilerSettings copy = new JpsKotlinCompilerSettings();
        copy.commonCompilerSettings = this.commonCompilerSettings;
        copy.k2JvmCompilerSettings = this.k2JvmCompilerSettings;
        copy.k2JsCompilerSettings = this.k2JsCompilerSettings;
        return copy;
    }

    @Override
    public void applyChanges(@NotNull JpsKotlinCompilerSettings modified) {
        // do nothing
    }

    @NotNull
    public static JpsKotlinCompilerSettings getSettings(@NotNull JpsProject project) {
        JpsKotlinCompilerSettings settings = project.getContainer().getChild(ROLE);
        if (settings == null) {
            settings = new JpsKotlinCompilerSettings();
        }
        return settings;
    }

    @NotNull
    public static JpsKotlinCompilerSettings getOrCreateSettings(@NotNull JpsProject project) {
        JpsKotlinCompilerSettings settings = project.getContainer().getChild(ROLE);
        if (settings == null) {
            settings = new JpsKotlinCompilerSettings();
            project.getContainer().setChild(ROLE, settings);
        }
        return settings;
    }

    @NotNull
    public static CommonCompilerArguments getCommonSettings(@NotNull JpsProject project) {
        return getSettings(project).commonCompilerSettings;
    }

    public static void setCommonSettings(@NotNull JpsProject project, @NotNull CommonCompilerArguments commonCompilerSettings) {
        getOrCreateSettings(project).commonCompilerSettings = commonCompilerSettings;
    }

    @NotNull
    public static K2JVMCompilerArguments getK2JvmSettings(@NotNull JpsProject project) {
        return getSettings(project).k2JvmCompilerSettings;
    }

    public static void setK2JvmSettings(@NotNull JpsProject project, @NotNull K2JVMCompilerArguments k2JvmCompilerSettings) {
        getOrCreateSettings(project).k2JvmCompilerSettings = k2JvmCompilerSettings;
    }

    @NotNull
    public static K2JSCompilerArguments getK2JsSettings(@NotNull JpsProject project) {
        return getSettings(project).k2JsCompilerSettings;
    }

    public static void setK2JsSettings(@NotNull JpsProject project, @NotNull K2JSCompilerArguments k2JsCompilerSettings) {
        getOrCreateSettings(project).k2JsCompilerSettings = k2JsCompilerSettings;
    }
}
