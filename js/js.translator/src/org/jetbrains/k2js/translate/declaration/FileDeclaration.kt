/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
package org.jetbrains.k2js.translate.declaration

import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer
import org.jetbrains.k2js.translate.initializer.InitializerVisitor
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.k2js.translate.initializer.InitializerUtils
import org.jetbrains.jet.lang.psi.JetObjectDeclaration
import org.jetbrains.jet.lang.psi.JetClass
import com.google.dart.compiler.backend.js.ast.JsFunction
import org.jetbrains.k2js.translate.utils.BindingUtils.*
import org.jetbrains.k2js.translate.initializer.InitializerUtils.*
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import com.intellij.util.SmartList

class FileDeclarationVisitor(
        val context: TranslationContext,
        initializers: List<JsPropertyInitializer> = SmartList()
) : DeclarationBodyVisitor(initializers, SmartList()) {

    private val initializer = JsAstUtils.createFunctionWithEmptyBody(context.scope())
    private val initializerContext = context.contextWithScope(initializer)
    private val initializerStatements = initializer.getBody()!!.getStatements()!!
    private val initializerVisitor = InitializerVisitor(initializerStatements)

    fun computeInitializer(): JsFunction? {
        if (initializerStatements.isEmpty()) {
            return null
        } else {
            return initializer
        }
    }

    public override fun visitClass(expression: JetClass, context: TranslationContext?): Void? {
        val classDescriptor = getClassDescriptor(context!!.bindingContext(), expression)
        val value = ClassTranslator(expression, context).translate()
        val entry = JsPropertyInitializer(context.getNameForDescriptor(classDescriptor).makeRef()!!, value)
        result.add(entry)
        return null
    }

    public override fun visitObjectDeclaration(declaration: JetObjectDeclaration, context: TranslationContext?): Void? {
        InitializerUtils.generateObjectInitializer(declaration, initializerStatements, context!!)
        return null
    }

    public override fun visitProperty(expression: JetProperty, context: TranslationContext?): Void? {
        context!! // hack

        super.visitProperty(expression, context)
        val initializer = expression.getInitializer()
        if (initializer != null) {
            val value = Translation.translateAsExpression(initializer, initializerContext)
            val propertyDescriptor: PropertyDescriptor = getPropertyDescriptor(context.bindingContext(), expression)
            initializerStatements.add(generateInitializerForProperty(context, propertyDescriptor, value))
        }

        val delegate = generateInitializerForDelegate(context, expression)
        if (delegate != null)
            initializerStatements.add(delegate)

        return null
    }

    public override fun visitAnonymousInitializer(expression: JetClassInitializer, context: TranslationContext?): Void? {
        expression.accept(initializerVisitor, initializerContext)
        return null
    }

}
