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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic


public object ProgressionCompanionFIF : CompositeFIF() {
    init {
        val numberProgressionConstructor = CallProgressionConstructorIntrinsic("NumberProgression")
        for (type in arrayOf(PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT)) {
            add(methodPattern("${type.typeName}Progression"), numberProgressionConstructor)
        }
        add(methodPattern("LongProgression"), CallProgressionConstructorIntrinsic("LongProgression"))
        add(methodPattern("CharProgression"), CallProgressionConstructorIntrinsic("CharProgression"))
    }

    private fun methodPattern(builtinProgressionName: String) = pattern("kotlin", builtinProgressionName, "Companion", "fromClosedRange")

    private class CallProgressionConstructorIntrinsic(val libraryProgressionName: String) : FunctionIntrinsic() {
        override fun apply(receiver: JsExpression?, arguments: MutableList<JsExpression>, context: TranslationContext): JsExpression
                = JsNew(JsNameRef(libraryProgressionName, Namer.KOTLIN_OBJECT_REF), arguments)
    }

}