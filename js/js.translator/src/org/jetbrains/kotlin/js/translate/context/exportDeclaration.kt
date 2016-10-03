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

@file:JvmName("DeclarationExporter")
package org.jetbrains.kotlin.js.translate.context

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils

fun StaticContext.exportDeclaration(declaration: DeclarationDescriptor, additionalStatements: MutableList<in JsStatement>): JsExpression? {
    return when {
        declaration is ClassDescriptor && declaration.kind == ClassKind.OBJECT -> {
            exportObject(declaration, additionalStatements)
            null
        }

        declaration is PropertyDescriptor -> {
            exportProperty(declaration, additionalStatements)
            null
        }

        else -> {
            getInnerNameForDescriptor(declaration).makeRef()
        }
    }
}

private fun StaticContext.exportObject(declaration: ClassDescriptor, additionalStatements: MutableList<in JsStatement>) {
    val suggestedName = nameSuggestion.suggest(declaration)!!
    val qualifier = getQualifiedReference(suggestedName.scope)
    val name = getNameForDescriptor(declaration)
    additionalStatements += JsAstUtils.defineGetter(program, qualifier, name.ident, getNameForObjectInstance(declaration).makeRef())
}

private fun StaticContext.exportProperty(declaration: PropertyDescriptor, additionalStatements: MutableList<in JsStatement>) {
    val propertyLiteral = JsObjectLiteral(true)

    val suggestedName = nameSuggestion.suggest(declaration)!!
    val qualifier = getQualifiedReference(suggestedName.scope)
    val name = getNameForDescriptor(declaration).ident

    val getterBody: JsExpression = if (JsDescriptorUtils.isSimpleFinalProperty(declaration)) {
        val accessToField = JsReturn(getInnerNameForDescriptor(declaration).makeRef())
        JsFunction(rootFunction.scope, JsBlock(accessToField), "$declaration getter")
    }
    else {
        getInnerNameForDescriptor(declaration.getter!!).makeRef()
    }
    propertyLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("get"), getterBody)

    if (declaration.isVar) {
        val setterBody: JsExpression = if (JsDescriptorUtils.isSimpleFinalProperty(declaration)) {
            val statements = mutableListOf<JsStatement>()
            val function = JsFunction(rootFunction.scope, JsBlock(statements), "$declaration setter")
            val valueName = function.scope.declareFreshName("value")
            function.parameters += JsParameter(valueName)
            statements += JsAstUtils.assignment(getInnerNameForDescriptor(declaration).makeRef(), valueName.makeRef()).makeStmt()
            function
        }
        else {
            getInnerNameForDescriptor(declaration.setter!!).makeRef()
        }
        propertyLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("set"), setterBody)
    }

    additionalStatements += JsAstUtils.defineProperty(qualifier, name, propertyLiteral, program).makeStmt()
}