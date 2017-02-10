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

package org.jetbrains.kotlin.js.translate.declaration

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class EnumTranslator(
        context: TranslationContext,
        val descriptor: ClassDescriptor,
        val entries: List<ClassDescriptor>
) : AbstractTranslator(context) {
    fun generateStandardMethods() {
        generateValuesFunction()
        generateValueOfFunction()
    }

    private fun generateValuesFunction() {
        val function = createFunction(DescriptorUtils.getFunctionByName(descriptor.staticScope, DescriptorUtils.ENUM_VALUES))

        val values = entries.map { JsInvocation(JsAstUtils.pureFqn(context().getNameForObjectInstance(it), null)) }
        function.body.statements += JsReturn(JsArrayLiteral(values))
    }

    private fun generateValueOfFunction() {
        val function = createFunction(DescriptorUtils.getFunctionByName(descriptor.staticScope, DescriptorUtils.ENUM_VALUE_OF))

        val nameParam = JsScope.declareTemporaryName("name")
        function.parameters += JsParameter(nameParam)

        val clauses = entries.map { entry ->
            JsCase().apply {
                caseExpression = context().program().getStringLiteral(entry.name.asString())
                statements += JsReturn(JsInvocation(JsAstUtils.pureFqn(context().getNameForObjectInstance(entry), null)))
            }
        }

        val message = JsBinaryOperation(JsBinaryOperator.ADD,
                context().program().getStringLiteral("No enum constant ${descriptor.fqNameSafe}."),
                nameParam.makeRef())
        val throwStatement = JsExpressionStatement(JsInvocation(Namer.throwIllegalStateExceptionFunRef(), message))

        if (clauses.isNotEmpty()) {
            val defaultCase = JsDefault().apply { statements += throwStatement }
            function.body.statements += JsSwitch(nameParam.makeRef(), clauses + defaultCase)
        }
        else {
            function.body.statements += throwStatement
        }

    }

    private fun createFunction(functionDescriptor: FunctionDescriptor): JsFunction {
        val function = context().getFunctionObject(functionDescriptor)
        function.name = context().getInnerNameForDescriptor(functionDescriptor)
        context().addDeclarationStatement(function.makeStmt())

        val classRef = context().getInnerReference(descriptor)
        val functionRef = function.name.makeRef()
        val assignment = JsAstUtils.assignment(JsNameRef(context().getNameForDescriptor(functionDescriptor), classRef), functionRef)
        context().addDeclarationStatement(assignment.makeStmt())

        return function
    }
}