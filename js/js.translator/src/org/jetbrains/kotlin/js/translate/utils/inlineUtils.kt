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

package org.jetbrains.kotlin.js.translate.utils

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.*
import org.jetbrains.kotlin.builtins.InlineStrategy
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.js.inline.util.isCallInvocation
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.reference.CallExpressionTranslator
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

/**
 * Recursively walks expression and sets metadata for all invocations of descriptor.
 *
 * When JetExpression is compiled, the resulting JsExpression
 * might not be JsInvocation.
 *
 * For example, extension call with nullable receiver:
 *  x?.fn(y)
 * will compile to:
 *  (x != null) ? fn.call(x, y) : null
 */
fun setInlineCallMetadata(
        expression: JsExpression,
        descriptor: CallableDescriptor,
        context: TranslationContext
) {
    assert(CallExpressionTranslator.shouldBeInlined(descriptor)) {
        "Expected descriptor of callable, that should be inlined, but got: $descriptor"
    }

    val name = context.aliasedName(descriptor)

    val visitor = object : RecursiveJsVisitor() {
        override fun visitInvocation(invocation: JsInvocation?) {
            if (invocation == null) return

            super.visitInvocation(invocation)

            if (name == invocation.name) {
                invocation.descriptor = descriptor
                invocation.inlineStrategy = InlineStrategy.IN_PLACE
            }
        }
    }

    visitor.accept(expression)
}

fun TranslationContext.aliasedName(descriptor: CallableDescriptor): JsName {
    val alias = getAliasForDescriptor(descriptor)
    val aliasName = (alias as? JsNameRef)?.getName()

    return aliasName ?: getNameForDescriptor(descriptor)
}

val JsExpression?.name: JsName?
    get() = when (this) {
        is JsInvocation -> {
            val qualifier = this.getQualifier()

            when {
                isCallInvocation(this) -> (qualifier as JsNameRef).getQualifier().name
                else -> qualifier.name
            }
        }
        is JsNameRef -> this.getName()
        else -> null
    }
