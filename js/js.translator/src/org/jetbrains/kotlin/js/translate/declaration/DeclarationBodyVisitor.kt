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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOwnParametersWithDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOrInheritsParametersWithDefaultValue

class DeclarationBodyVisitor(
        private val containingClass: ClassDescriptor,
        private val context: TranslationContext,
        private val enumInitializer: JsFunction?
) : AbstractDeclarationVisitor() {
    private var enumEntryOrdinal: Int = 0
    val initializerStatements = mutableListOf<JsStatement>()
    val enumEntries = mutableListOf<ClassDescriptor>()

    override fun visitClassOrObject(classOrObject: KtClassOrObject, context: TranslationContext) {
        super.visitClassOrObject(classOrObject, context)

        if (classOrObject is KtObjectDeclaration) {
            if (classOrObject.isCompanion()) {
                val descriptor = BindingUtils.getDescriptorForElement(context.bindingContext(), classOrObject) as ClassDescriptor
                addInitializerStatement(JsInvocation(context.getNameForObjectInstance(descriptor).makeRef()).makeStmt())
            }
        }
    }

    override fun visitEnumEntry(enumEntry: KtEnumEntry, context: TranslationContext) {
        val enumInitializer = this.enumInitializer!!
        val descriptor = getClassDescriptor(context.bindingContext(), enumEntry)
        val supertypes = getSupertypesWithoutFakes(descriptor)
        enumEntries += descriptor

        if (enumEntry.getBody() != null || supertypes.size > 1) {
            ClassTranslator.translate(enumEntry, context, enumInitializer.name, enumEntryOrdinal)
            enumInitializer.body.statements += JsNew(context.getInnerReference(descriptor)).makeStmt()
        }
        else {
            val enumName = context.getInnerNameForDescriptor(descriptor)
            val enumInstanceName = context.createGlobalName(enumName.ident + "_instance")

            assert(supertypes.size == 1) { "Simple Enum entry must have one supertype" }
            val jsEnumEntryCreation = ClassInitializerTranslator.generateEnumEntryInstanceCreation(context, enumEntry, enumEntryOrdinal)
            context.addDeclarationStatement(JsAstUtils.newVar(enumInstanceName, null))
            enumInitializer.body.statements += JsAstUtils.assignment(pureFqn(enumInstanceName, null), jsEnumEntryCreation).makeStmt()

            val enumInstanceFunction = context.createRootScopedFunction(descriptor)
            enumInstanceFunction.name = context.getNameForObjectInstance(descriptor)
            context.addDeclarationStatement(enumInstanceFunction.makeStmt())

            enumInstanceFunction.body.statements += JsInvocation(pureFqn(enumInitializer.name, null)).makeStmt()
            enumInstanceFunction.body.statements += JsReturn(enumInstanceName.makeRef())
        }

        context.export(descriptor)

        enumEntryOrdinal++
    }

    override fun visitAnonymousInitializer(expression: KtAnonymousInitializer, context: TranslationContext) {
        // parsed it in initializer visitor => no additional actions are needed
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: TranslationContext) { }

    private fun addInitializerStatement(statement: JsStatement) {
        initializerStatements.add(statement)
    }

    override fun addFunction(descriptor: FunctionDescriptor, expression: JsExpression?) {
        if (!descriptor.hasOrInheritsParametersWithDefaultValue() || !descriptor.isOverridableOrOverrides) {
            if (expression != null) {
                context.addFunctionToPrototype(containingClass, descriptor, expression)
            }
        }
        else {
            val bodyName = context.scope().declareName(
                    context.getNameForDescriptor(descriptor).ident + Namer.DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX)
            if (expression != null) {
                val prototypeRef = JsAstUtils.prototypeOf(context.getInnerReference(containingClass))
                val functionRef = JsNameRef(bodyName, prototypeRef)
                context.addDeclarationStatement(JsAstUtils.assignment(functionRef, expression).makeStmt())
            }

            if (descriptor.hasOwnParametersWithDefaultValue()) {
                val caller = JsFunction(context.getScopeForDescriptor(containingClass), JsBlock(), "")
                val callerContext = context
                        .newDeclaration(descriptor)
                        .translateAndAliasParameters(descriptor, caller.parameters)
                        .innerBlock(caller.body)

                val callbackName = caller.scope.declareTemporaryName("callback" + Namer.DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX)
                val callee = JsNameRef(bodyName, JsLiteral.THIS)

                val defaultInvocation = JsInvocation(callee, java.util.ArrayList<JsExpression>())
                val callbackInvocation = JsInvocation(callbackName.makeRef())
                val chosenInvocation = JsConditional(callbackName.makeRef(), callbackInvocation, defaultInvocation)
                defaultInvocation.arguments += caller.parameters.map { it.name.makeRef() }
                callbackInvocation.arguments += defaultInvocation.arguments.map { it.deepCopy() }
                caller.parameters.add(JsParameter(callbackName))

                caller.body.statements += FunctionBodyTranslator.setDefaultValueForArguments(descriptor, callerContext)

                val returnType = descriptor.returnType!!
                val statement = if (KotlinBuiltIns.isUnit(returnType)) chosenInvocation.makeStmt() else JsReturn(chosenInvocation)
                caller.body.statements += statement

                context.addFunctionToPrototype(containingClass, descriptor, caller)
            }
        }
    }

    override fun addProperty(descriptor: PropertyDescriptor, getter: JsExpression, setter: JsExpression?) {
        if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor)) {
            val literal = JsObjectLiteral(true)
            literal.propertyInitializers += JsPropertyInitializer(context.program().getStringLiteral("get"), getter)
            if (setter != null) {
                literal.propertyInitializers += JsPropertyInitializer(context.program().getStringLiteral("set"), setter)
            }
            context.addAccessorsToPrototype(containingClass, descriptor, literal)
        }
    }

    override fun getBackingFieldReference(descriptor: PropertyDescriptor): JsExpression {
        return Namer.getDelegateNameRef(descriptor.name.asString())
    }
}
