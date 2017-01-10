/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.psiElement

class DummyAccessorInvocationTransformer : JsVisitorWithContextImpl() {
    override fun endVisit(x: JsNameRef, ctx: JsContext<in JsNode>) {
        super.endVisit(x, ctx)
        val dummy = tryCreatePropertyGetterInvocation(x)
        if (dummy != null) {
            ctx.replaceMe(dummy)
        }
    }

    override fun endVisit(x: JsBinaryOperation, ctx: JsContext<in JsNode>) {
        super.endVisit(x, ctx)
        val dummy = tryCreatePropertySetterInvocation(x)
        if (dummy != null) {
            ctx.replaceMe(dummy)
        }
    }

    private fun tryCreatePropertyGetterInvocation(x: JsNameRef): JsInvocation? {
        if (x.inlineStrategy != null && x.descriptor is PropertyGetterDescriptor) {
            val dummyInvocation = JsInvocation(x)
            copyInlineMetadata(x, dummyInvocation)
            return dummyInvocation
        }
        return null
    }

    private fun tryCreatePropertySetterInvocation(x: JsBinaryOperation): JsInvocation? {
        if (!x.operator.isAssignment || x.arg1 !is JsNameRef) return null
        val name = x.arg1 as JsNameRef
        if (name.inlineStrategy != null && name.descriptor is PropertySetterDescriptor) {
            val dummyInvocation = JsInvocation(name, x.arg2)
            copyInlineMetadata(name, dummyInvocation)
            return dummyInvocation
        }
        return null
    }

    private fun copyInlineMetadata(from: JsNameRef, to: JsInvocation) {
        to.inlineStrategy = from.inlineStrategy
        to.descriptor = from.descriptor
        to.psiElement = from.psiElement
    }
}
