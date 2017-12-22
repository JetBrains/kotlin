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

package org.jetbrains.kotlinx.serialization.compiler.backend.js

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils

internal class JsBlockBuilder {
    val block: JsBlock = JsBlock()
    operator fun JsStatement.unaryPlus() {
        block.statements.add(this)
    }

    val body: List<JsStatement>
        get() = block.statements
}

internal fun JsBlockBuilder.jsWhile(condition: JsExpression, body: JsBlockBuilder.() -> Unit, label: JsLabel? = null) {
    val b = JsBlockBuilder()
    b.body()
    val w = JsWhile(condition, b.block)
    if (label == null) {
        +w
    } else {
        label.statement = w
        +label
    }
}

internal class JsCasesBuilder() {
    val caseList: MutableList<JsSwitchMember> = mutableListOf()
    operator fun JsSwitchMember.unaryPlus() {
        caseList.add(this)
    }
}

internal fun JsCasesBuilder.case(condition: JsExpression, body: JsBlockBuilder.() -> Unit) {
    val a = JsCase()
    a.caseExpression = condition
    val b = JsBlockBuilder()
    b.body()
    a.statements += b.body
    +a
}

internal fun JsCasesBuilder.default(body: JsBlockBuilder.() -> Unit) {
    val a = JsDefault()
    val b = JsBlockBuilder()
    b.body()
    a.statements += b.body
    +a
}

internal fun JsBlockBuilder.jsSwitch(condition: JsExpression, cases: JsCasesBuilder.() -> Unit) {
    val b = JsCasesBuilder()
    b.cases()
    val sw = JsSwitch(condition, b.caseList)
    +sw
}

internal fun TranslationContext.buildFunction(descriptor: FunctionDescriptor, bodyGen: JsBlockBuilder.(JsFunction, TranslationContext) -> Unit): JsFunction {
    val functionObject = this.getFunctionObject(descriptor)
    val innerCtx = this.newDeclaration(descriptor).translateAndAliasParameters(descriptor, functionObject.parameters)
    val b = JsBlockBuilder()
    b.bodyGen(functionObject, innerCtx)
    functionObject.body.statements += b.body
    return functionObject
}

internal fun propNotSeenTest(seenVar: JsNameRef, index: Int): JsBinaryOperation = JsAstUtils.equality(
        JsBinaryOperation(
                JsBinaryOperator.BIT_AND,
                seenVar,
                JsIntLiteral(1 shl (index % 32))
        ),
        JsIntLiteral(0)
)

internal fun TranslationContext.serializerObjectGetter(serializer: ClassDescriptor): JsExpression {
    return ReferenceTranslator.translateAsValueReference(serializer, this)
}

internal fun TranslationContext.translateQualifiedReference(clazz: ClassDescriptor): JsExpression {
    return ReferenceTranslator.translateAsTypeReference(clazz, this)
}