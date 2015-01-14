/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.kotlin.config.CompilerSettings;

public class JpsKotlinCompilerSettings extends JpsElementBase<JpsKotlinCompilerSettings> {
    static final JpsElementChildRole<JpsKotlinCompilerSettings> ROLE = JpsElementChildRoleBase.create("Kotlin Compiler Settings");

    @NotNull
    private CommonCompilerArguments commonCompilerArguments = new CommonCompilerArguments.DummyImpl();
    @NotNull
    private K2JVMCompilerArguments k2JvmCompilerArguments = new K2JVMCompilerArguments();
    @NotNull
    private K2JSCompilerArguments k2JsCompilerArguments = new K2JSCompilerArguments();
    @NotNull
    private CompilerSettings compilerSettings = new CompilerSettings();

    @NotNull
    @Override
    public JpsKotlinCompilerSettings createCopy() {
        JpsKotlinCompilerSettings copy = new JpsKotlinCompilerSettings();
        copy.commonCompilerArguments = this.commonCompilerArguments;
        copy.k2JvmCompilerArguments = this.k2JvmCompilerArguments;
        copy.k2JsCompilerArguments = this.k2JsCompilerArguments;
        copy.compilerSettings = this.compilerSettings;
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
    public static CommonCompilerArguments getCommonCompilerArguments(@NotNull JpsProject project) {
        return getSettings(project).commonCompilerArguments;
    }

    public static void setCommonCompilerArguments(@NotNull JpsProject project, @NotNull CommonCompilerArguments commonCompilerSettings) {
        getOrCreateSettings(project).commonCompilerArguments = commonCompilerSettings;
    }

    @NotNull
    public static K2JVMCompilerArguments getK2JvmCompilerArguments(@NotNull JpsProject project) {
        return getSettings(project).k2JvmCompilerArguments;
    }

    public static void setK2JvmCompilerArguments(@NotNull JpsProject project, @NotNull K2JVMCompilerArguments k2JvmCompilerArguments) {
        getOrCreateSettings(project).k2JvmCompilerArguments = k2JvmCompilerArguments;
    }

    @NotNull
    public static K2JSCompilerArguments getK2JsCompilerArguments(@NotNull JpsProject project) {
        return getSettings(project).k2JsCompilerArguments;
    }

    public static void setK2JsCompilerArguments(@NotNull JpsProject project, @NotNull K2JSCompilerArguments k2JsCompilerArguments) {
        getOrCreateSettings(project).k2JsCompilerArguments = k2JsCompilerArguments;
    }

    @NotNull
    public static CompilerSettings getCompilerSettings(@NotNull JpsProject project) {
        return getSettings(project).compilerSettings;
    }

    public static void setCompilerSettings(@NotNull JpsProject project, @NotNull CompilerSettings compilerSettings) {
        getOrCreateSettings(project).compilerSettings = compilerSettings;
    }
}
