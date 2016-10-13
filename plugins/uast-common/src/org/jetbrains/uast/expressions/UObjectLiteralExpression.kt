/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

import com.intellij.psi.PsiType
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.visitor.UastVisitor

/**
 * Represents an object literal expression, e.g. `new Runnable() {}` in Java.
 */
interface UObjectLiteralExpression : UCallExpression {
    /**
     * Returns the class declaration.
     */
    val declaration: UClass

    override val methodIdentifier: UIdentifier?
        get() = null
    
    override val kind: UastCallKind
        get() = UastCallKind.CONSTRUCTOR_CALL

    override val methodName: String?
        get() = null

    override val receiver: UExpression?
        get() = null
    
    override val receiverType: PsiType?
        get() = null

    override val returnType: PsiType?
        get() = null
    
    
    override fun accept(visitor: UastVisitor) {
        if (visitor.visitObjectLiteralExpression(this)) return
        declaration.accept(visitor)
        visitor.afterVisitObjectLiteralExpression(this)
    }

    override fun asLogString() = log("UObjectLiteralExpression", declaration)
    override fun asRenderString() = "anonymous " + declaration.text
}