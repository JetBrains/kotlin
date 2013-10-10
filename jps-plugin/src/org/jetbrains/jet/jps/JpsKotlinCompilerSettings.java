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

import com.intellij.util.xmlb.Accessor;
import com.intellij.util.xmlb.XmlSerializerUtil;
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
    public CommonCompilerArguments commonCompilerSettings = CommonCompilerArguments.DUMMY;
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
        JpsKotlinCompilerSettings settings = getSettings(project);
        return settings.commonCompilerSettings;
    }

    public static void setCommonSettings(@NotNull JpsProject project, @NotNull CommonCompilerArguments commonCompilerSettings) {
        JpsKotlinCompilerSettings settings = getOrCreateSettings(project);
        settings.commonCompilerSettings = commonCompilerSettings;
    }

    @NotNull
    public static K2JVMCompilerArguments getMergedK2JvmSettings(@NotNull JpsProject project) {
        JpsKotlinCompilerSettings settings = getSettings(project);
        return merge(settings.commonCompilerSettings, settings.k2JvmCompilerSettings);
    }

    public static void setK2JvmSettings(@NotNull JpsProject project, @NotNull K2JVMCompilerArguments k2JvmCompilerSettings) {
        JpsKotlinCompilerSettings settings = getOrCreateSettings(project);
        settings.k2JvmCompilerSettings = k2JvmCompilerSettings;
    }

    @NotNull
    public static K2JSCompilerArguments getMergedK2JsSettings(@NotNull JpsProject project) {
        JpsKotlinCompilerSettings settings = getSettings(project);
        return merge(settings.commonCompilerSettings, settings.k2JsCompilerSettings);
    }

    public static void setK2JsSettings(@NotNull JpsProject project, @NotNull K2JSCompilerArguments k2JsCompilerSettings) {
        JpsKotlinCompilerSettings settings = getOrCreateSettings(project);
        settings.k2JsCompilerSettings = k2JsCompilerSettings;
    }

    @NotNull
    private static <T extends CommonCompilerArguments> T merge(@NotNull CommonCompilerArguments from, @NotNull T to) {
        Class<CommonCompilerArguments> fromClass = CommonCompilerArguments.class;

        assert fromClass.isAssignableFrom(to.getClass()) : to.getClass() + " is not assignable to " + fromClass;

        T mergedCopy = XmlSerializerUtil.createCopy(to);

        for (Accessor accessor : XmlSerializerUtil.getAccessors(fromClass)) {
            accessor.write(mergedCopy, accessor.read(from));
        }

        return mergedCopy;
    }
}
