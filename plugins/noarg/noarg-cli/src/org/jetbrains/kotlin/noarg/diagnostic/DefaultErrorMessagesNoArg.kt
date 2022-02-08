/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.noarg.diagnostic

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

object DefaultErrorMessagesNoArg : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("AnnotationProcessing")
    override fun getMap() = MAP

    init {
        MAP.put(ErrorsNoArg.NO_NOARG_CONSTRUCTOR_IN_SUPERCLASS, "Zero-argument constructor was not found in the superclass")
        MAP.put(
            ErrorsNoArg.NOARG_ON_INNER_CLASS,
            "Noarg constructor generation for inner classes is deprecated and will be prohibited soon"
        )
        MAP.put(ErrorsNoArg.NOARG_ON_INNER_CLASS_ERROR, "Noarg constructor generation is not possible for inner classes")
        MAP.put(
            ErrorsNoArg.NOARG_ON_LOCAL_CLASS,
            "Noarg constructor generation for local classes is deprecated and will be prohibited soon"
        )
        MAP.put(ErrorsNoArg.NOARG_ON_LOCAL_CLASS_ERROR, "Noarg constructor generation is not possible for local classes")
    }
}
