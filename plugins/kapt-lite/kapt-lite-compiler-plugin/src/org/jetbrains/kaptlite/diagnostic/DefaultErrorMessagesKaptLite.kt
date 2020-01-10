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

package org.jetbrains.kaptlite.diagnostic

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

object DefaultErrorMessagesKaptLite : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("KaptLite")
    override fun getMap() = MAP

    init {
        MAP.put(
            ErrorsKaptLite.KAPT_INCOMPATIBLE_NAME,
            "Name ''{0}'' is forbidden in kapt. Use only Java-compatible names",
            Renderers.TO_STRING
        )

        MAP.put(
            ErrorsKaptLite.KAPT_NESTED_NAME_CLASH,
            "Class name ''{0}'' is forbidden in kapt. Java prohibits name clashes with outer classes",
            Renderers.TO_STRING
        )

        MAP.put(
            ErrorsKaptLite.TIME,
            "Time spent in stub generation: ''{0}''",
            Renderers.TO_STRING
        )
    }
}