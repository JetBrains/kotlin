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

package org.jetbrains.kotlin.jps.model

import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION
import org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings

internal class KotlinCommonCompilerArgumentsSerializer : JpsProjectExtensionSerializer(KOTLIN_COMPILER_SETTINGS_FILE,
                                                                                       KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION) {

    override fun loadExtension(project: JpsProject, componentTag: Element) {
        val settings = XmlSerializer.deserialize(componentTag, CommonCompilerArguments.DummyImpl::class.java)
        if (VersionComparatorUtil.compare(settings.languageVersion, settings.apiVersion) < 0) {
            settings.apiVersion = settings.languageVersion
        }
        JpsKotlinCompilerSettings.setCommonCompilerArguments(project, settings)
    }

    override fun saveExtension(project: JpsProject, componentTag: Element) {
    }
}
