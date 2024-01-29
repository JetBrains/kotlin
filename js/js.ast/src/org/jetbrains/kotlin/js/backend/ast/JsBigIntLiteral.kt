/*
 * Copyright 2010-2024 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.backend.ast;

import java.math.BigInteger

class JsBigIntLiteral(val value: BigInteger) : JsNumberLiteral() {
    override fun accept(visitor: JsVisitor) {
        visitor.visitBigInt(this)
    }

    override fun toString() = value.toString()

    override fun traverse(visitor: JsVisitorWithContext, ctx: JsContext<*>) {
        visitor.visit(this@JsBigIntLiteral, ctx)
        visitor.endVisit(this@JsBigIntLiteral, ctx)
    }

    override fun deepCopy(): JsExpression {
        return JsBigIntLiteral(value).withMetadataFrom(this)
    }
}
