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

package org.jetbrains.kotlin.jps

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings

class JpsKotlinCompilerSettings : JpsElementBase<JpsKotlinCompilerSettings>() {
    private var commonCompilerArguments: CommonCompilerArguments = CommonCompilerArguments.DummyImpl()
    private var k2JvmCompilerArguments = K2JVMCompilerArguments()
    private var k2JsCompilerArguments = K2JSCompilerArguments()
    private var compilerSettings = CompilerSettings()

    override fun createCopy(): JpsKotlinCompilerSettings {
        val copy = JpsKotlinCompilerSettings()
        copy.commonCompilerArguments = this.commonCompilerArguments
        copy.k2JvmCompilerArguments = this.k2JvmCompilerArguments
        copy.k2JsCompilerArguments = this.k2JsCompilerArguments
        copy.compilerSettings = this.compilerSettings
        return copy
    }

    override fun applyChanges(modified: JpsKotlinCompilerSettings) {
        // do nothing
    }

    companion object {
        internal val ROLE = JpsElementChildRoleBase.create<JpsKotlinCompilerSettings>("Kotlin Compiler Settings")

        fun getSettings(project: JpsProject) = project.container.getChild(ROLE) ?: JpsKotlinCompilerSettings()

        fun getOrCreateSettings(project: JpsProject): JpsKotlinCompilerSettings {
            var settings = project.container.getChild(ROLE)
            if (settings == null) {
                settings = JpsKotlinCompilerSettings()
                project.container.setChild(ROLE, settings)
            }
            return settings
        }

        fun getCommonCompilerArguments(project: JpsProject) = getSettings(project).commonCompilerArguments

        fun setCommonCompilerArguments(project: JpsProject, commonCompilerSettings: CommonCompilerArguments) {
            getOrCreateSettings(project).commonCompilerArguments = commonCompilerSettings
        }

        fun getK2JvmCompilerArguments(project: JpsProject) = getSettings(project).k2JvmCompilerArguments

        fun setK2JvmCompilerArguments(project: JpsProject, k2JvmCompilerArguments: K2JVMCompilerArguments) {
            getOrCreateSettings(project).k2JvmCompilerArguments = k2JvmCompilerArguments
        }

        fun getK2JsCompilerArguments(project: JpsProject) = getSettings(project).k2JsCompilerArguments

        fun setK2JsCompilerArguments(project: JpsProject, k2JsCompilerArguments: K2JSCompilerArguments) {
            getOrCreateSettings(project).k2JsCompilerArguments = k2JsCompilerArguments
        }

        fun getCompilerSettings(project: JpsProject) = getSettings(project).compilerSettings

        fun setCompilerSettings(project: JpsProject, compilerSettings: CompilerSettings) {
            getOrCreateSettings(project).compilerSettings = compilerSettings
        }
    }
}
