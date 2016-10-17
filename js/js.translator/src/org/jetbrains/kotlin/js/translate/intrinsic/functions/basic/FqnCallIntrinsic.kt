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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.basic

import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class FqnCallIntrinsic(
        simpleName: String,
        fqn: String,
        isConstructor: Boolean = false,
        receiverAsArgument: Boolean = true
) : LazyImportedCallIntrinsic(simpleName, isConstructor = isConstructor, receiverAsArgument = receiverAsArgument) {
    val fqn = FqName(fqn)

    override fun getExpression(context: TranslationContext): JsExpression {
        val packageDescriptor = context.currentModule.getPackage(fqn.parent())

        // Hack to compile stdlib
        val descriptors = packageDescriptor.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL) {
            it.asString() == fqn.shortName().asString()
        }

        return if (descriptors.size == 1) {
            JsAstUtils.replaceRootReference(context.getQualifiedReference(fqn), Namer.kotlinObject())
        }
        else {
            ReferenceTranslator.translateAsValueReference(descriptors.first(), context)
        }
    }
}