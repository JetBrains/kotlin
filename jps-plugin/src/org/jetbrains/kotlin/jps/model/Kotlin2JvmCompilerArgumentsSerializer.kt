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

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.SettingConstants.KOTLIN_TO_JVM_COMPILER_ARGUMENTS_SECTION
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings

internal class Kotlin2JvmCompilerArgumentsSerializer : BaseJpsCompilerSettingsSerializer<K2JVMCompilerArguments>(
        KOTLIN_TO_JVM_COMPILER_ARGUMENTS_SECTION, ::K2JVMCompilerArguments
) {
    override fun onLoad(project: JpsProject, settings: K2JVMCompilerArguments) {
        JpsKotlinCompilerSettings.setK2JvmCompilerArguments(project, settings)
    }
}
