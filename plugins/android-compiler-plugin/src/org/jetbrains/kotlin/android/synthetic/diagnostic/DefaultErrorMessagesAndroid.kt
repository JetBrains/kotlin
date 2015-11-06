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

package org.jetbrains.kotlin.android.synthetic.diagnostic

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

public class DefaultErrorMessagesAndroid : DefaultErrorMessages.Extension {

    private companion object {
        val MAP = DiagnosticFactoryToRendererMap()

        init {
            MAP.put(ErrorsAndroid.SYNTHETIC_INVALID_WIDGET_TYPE,
                    "Widget has an invalid type ''{0}''. Please specify the fully qualified widget class name in XML",
                    Renderers.TO_STRING)

            MAP.put(ErrorsAndroid.SYNTHETIC_UNRESOLVED_WIDGET_TYPE,
                    "Widget has an unresolved type ''{0}'', and thus it was upcasted to ''android.view.View''",
                    Renderers.TO_STRING)

            MAP.put(ErrorsAndroid.SYNTHETIC_DEPRECATED_PACKAGE,
                    "Use properties from the build variant packages")
        }
    }

    override fun getMap() = MAP
}