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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes
import org.jetbrains.kotlin.psi.*
import java.util.*

class DeclarationBodyVisitor(
        private val containingClass: ClassDescriptor,
        private val context: TranslationContext
) : AbstractDeclarationVisitor() {
    private var enumEntryOrdinal: Int = 0
    val initializerStatements = ArrayList<JsStatement>()

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
        val descriptor = getClassDescriptor(context.bindingContext(), enumEntry)
        val supertypes = getSupertypesWithoutFakes(descriptor)

        if (enumEntry.getBody() != null || supertypes.size > 1) {
            ClassTranslator.translate(enumEntry, context)
        }
        else {
            // Simplify by omitting _getInstance() function
            val enumName = context.getInnerNameForDescriptor(descriptor)
            val enumInstanceName = context.createGlobalName(enumName.ident + "_instance")

            assert(supertypes.size == 1) { "Simple Enum entry must have one supertype" }
            val jsEnumEntryCreation = ClassInitializerTranslator
                    .generateEnumEntryInstanceCreation(context, supertypes[0], enumEntry, enumEntryOrdinal)
            context.addDeclarationStatement(JsAstUtils.newVar(enumInstanceName, jsEnumEntryCreation))
            val jsEnumEntryFunction = context.createTopLevelAnonymousFunction(descriptor)
            jsEnumEntryFunction.body.statements.add(JsReturn(jsEnumEntryCreation))

            val enumInstanceFunction = context.createTopLevelAnonymousFunction(descriptor)
            enumInstanceFunction.name = context.getNameForObjectInstance(descriptor)
            context.addDeclarationStatement(enumInstanceFunction.makeStmt())

            enumInstanceFunction.body.statements.add(JsReturn(enumInstanceName.makeRef()))
        }

        enumEntryOrdinal++
    }

    override fun visitAnonymousInitializer(expression: KtAnonymousInitializer, context: TranslationContext) {
        // parsed it in initializer visitor => no additional actions are needed
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: TranslationContext) { }

    private fun addInitializerStatement(statement: JsStatement) {
        initializerStatements.add(statement)
    }

    override fun addClass(descriptor: ClassDescriptor) {
        context.export(descriptor)
    }

    override fun addFunction(descriptor: FunctionDescriptor, expression: JsExpression) {
        context.addFunctionToPrototype(containingClass, descriptor, expression)
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
