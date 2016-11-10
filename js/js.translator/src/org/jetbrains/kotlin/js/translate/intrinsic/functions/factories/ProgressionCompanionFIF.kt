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

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsNew
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator

object ProgressionCompanionFIF : FunctionIntrinsicFactory {
    val patterns = listOf(methodPattern("IntProgression"), methodPattern("LongProgression"), methodPattern("CharProgression"))

    private fun methodPattern(builtinProgressionName: String) =
            pattern("kotlin.ranges", builtinProgressionName, "Companion", "fromClosedRange")

    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? =
        if (patterns.any { it.apply(descriptor) }) {
            FromClosedRangeIntrinsic(descriptor)
        }
        else {
            null
        }

    private class FromClosedRangeIntrinsic(descriptor: FunctionDescriptor) : FunctionIntrinsic() {
        val progressionType = descriptor.containingDeclaration.containingDeclaration as ClassDescriptor

        override fun apply(receiver: JsExpression?, arguments: MutableList<JsExpression>, context: TranslationContext): JsExpression {
            val constructor = ReferenceTranslator.translateAsTypeReference(progressionType, context)
            return JsNew(constructor, arguments)
        }
    }
}