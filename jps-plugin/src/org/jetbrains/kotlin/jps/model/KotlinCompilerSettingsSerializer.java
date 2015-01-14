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

package org.jetbrains.kotlin.jps.model;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer;
import org.jetbrains.kotlin.config.CompilerSettings;
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings;

import static org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE;
import static org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMPILER_SETTINGS_SECTION;

class KotlinCompilerSettingsSerializer extends JpsProjectExtensionSerializer {
    KotlinCompilerSettingsSerializer() {
        super(KOTLIN_COMPILER_SETTINGS_FILE, KOTLIN_COMPILER_SETTINGS_SECTION);
    }

    @Override
    public void loadExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
        CompilerSettings settings = XmlSerializer.deserialize(componentTag, CompilerSettings.class);
        if (settings == null) {
            settings = new CompilerSettings();
        }

        JpsKotlinCompilerSettings.setCompilerSettings(project, settings);
    }

    @Override
    public void saveExtension(@NotNull JpsProject project, @NotNull Element componentTag) {
    }
}
