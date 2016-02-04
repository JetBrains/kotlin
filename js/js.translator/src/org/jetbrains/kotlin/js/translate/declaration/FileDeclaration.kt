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
package org.jetbrains.kotlin.js.translate.declaration

import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.initializer.InitializerUtils
import org.jetbrains.kotlin.js.translate.initializer.InitializerUtils.generateInitializerForDelegate
import org.jetbrains.kotlin.js.translate.initializer.InitializerUtils.generateInitializerForProperty
import org.jetbrains.kotlin.js.translate.initializer.InitializerVisitor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptor
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty

class FileDeclarationVisitor(
        val context: TranslationContext,
        initializers: List<JsPropertyInitializer> = SmartList()
) : DeclarationBodyVisitor(initializers, SmartList()) {

    private val initializer = JsAstUtils.createFunctionWithEmptyBody(context.scope())
    private val initializerContext = context.contextWithScope(initializer)
    private val initializerStatements = initializer.body.statements
    private val initializerVisitor = InitializerVisitor(initializerStatements)

    fun computeInitializer(): JsFunction? {
        if (initializerStatements.isEmpty()) {
            return null
        } else {
            return initializer
        }
    }

    override fun visitClass(declaration: KtClass, context: TranslationContext?): Void? {
        result.addAll(ClassTranslator.translate(declaration, context!!))
        return null
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, context: TranslationContext?): Void? {
        InitializerUtils.generateObjectInitializer(declaration, initializerStatements, context!!)
        return null
    }

    override fun visitProperty(expression: KtProperty, context: TranslationContext?): Void? {
        context!! // hack

        super.visitProperty(expression, context)
        val initializer = expression.initializer
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

    override fun visitAnonymousInitializer(expression: KtAnonymousInitializer, context: TranslationContext?): Void? {
        expression.accept(initializerVisitor, initializerContext)
        return null
    }
}
