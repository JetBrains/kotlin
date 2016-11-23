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

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

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
        val function = createFunction(getEnumFunction(DescriptorUtils.ENUM_VALUES))

        val values = entries.map { JsInvocation(JsAstUtils.pureFqn(context().getNameForObjectInstance(it), null)) }
        function.body.statements += JsReturn(JsArrayLiteral(values))
    }

    private fun generateValueOfFunction() {
        val function = createFunction(getEnumFunction(DescriptorUtils.ENUM_VALUE_OF))

        val nameParam = function.scope.declareTemporaryName("name")
        function.parameters += JsParameter(nameParam)

        val clauses = entries.map { entry ->
            JsCase().apply {
                caseExpression = context().program().getStringLiteral(entry.name.asString())
                statements += JsReturn(JsInvocation(JsAstUtils.pureFqn(context().getNameForObjectInstance(entry), null)))
            }
        }

        if (clauses.isNotEmpty()) {
            function.body.statements += JsSwitch(nameParam.makeRef(), clauses)
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

    private fun getEnumFunction(name: Name): FunctionDescriptor {
        val functions = descriptor.staticScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
        return functions
                .mapNotNull { (it as? FunctionDescriptor)?.original }
                .first { it.name == name }
    }
}