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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.preloading.ClassCondition
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.Companion.NO_LOCATION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR

class CompilerEnvironment private constructor(
        val kotlinPaths: KotlinPaths,
        val classesToLoadByParent: ClassCondition,
        val services: Services
) {

    fun success(): Boolean {
        return kotlinPaths.homePath.exists()
    }

    fun reportErrorsTo(messageCollector: MessageCollector) {
        if (!kotlinPaths.homePath.exists()) {
            messageCollector.report(ERROR, "Cannot find kotlinc home: " + kotlinPaths.homePath + ". Make sure plugin is properly installed, " +
                                           "or specify " + PathUtil.JPS_KOTLIN_HOME_PROPERTY + " system property", NO_LOCATION)
        }
    }

    companion object {
        fun getEnvironmentFor(
                kotlinPaths: KotlinPaths,
                classesToLoadByParent: ClassCondition,
                compilerServices: Services
        ): CompilerEnvironment {
            return CompilerEnvironment(kotlinPaths, classesToLoadByParent, compilerServices)
        }
    }
}
