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

package org.jetbrains.kotlin.js.translate.context

import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.isInlineOnlyOrReifiable
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isLibraryObject
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.assignment
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class DeclarationExporter(val context: StaticContext) {
    private val objectLikeKinds = setOf(ClassKind.OBJECT, ClassKind.ENUM_ENTRY)
    private val exportedDeclarations = mutableSetOf<MemberDescriptor>()
    private val localPackageNames = mutableMapOf<FqName, JsName>()
    val statements = mutableListOf<JsStatement>()

    fun export(descriptor: MemberDescriptor, force: Boolean) {
        if (exportedDeclarations.contains(descriptor)) return
        if (descriptor is ConstructorDescriptor && descriptor.isPrimary) return
        if (isNativeObject(descriptor) || isLibraryObject(descriptor)) return
        if (descriptor.isInlineOnlyOrReifiable()) return

        val suggestedName = context.nameSuggestion.suggest(descriptor) ?: return

        val container = suggestedName.scope
        if (!descriptor.shouldBeExported(force)) return
        exportedDeclarations.add(descriptor)

        val qualifier = when {
            container is PackageFragmentDescriptor -> {
                getLocalPackageReference(container.fqName)
            }
            DescriptorUtils.isObject(container) -> {
                JsAstUtils.prototypeOf(context.getInnerNameForDescriptor(container).makeRef())
            }
            else -> {
                context.getInnerNameForDescriptor(container).makeRef()
            }
        }

        when {
            descriptor is ClassDescriptor && descriptor.kind in objectLikeKinds -> {
                exportObject(descriptor, qualifier)
            }
            descriptor is PropertyDescriptor && container is PackageFragmentDescriptor -> {
                exportProperty(descriptor, qualifier)
            }
            else -> {
                assign(descriptor, qualifier, context.getInnerNameForDescriptor(descriptor).makeRef())
            }
        }
    }

    private fun assign(descriptor: DeclarationDescriptor, qualifier: JsExpression, expression: JsExpression) {
        val propertyName = context.getNameForDescriptor(descriptor)
        if (propertyName.staticRef == null) {
            if (expression !is JsNameRef || expression.name !== propertyName) {
                propertyName.staticRef = expression
            }
        }
        statements += assignment(JsNameRef(propertyName, qualifier), expression).makeStmt()
    }

    private fun exportObject(declaration: ClassDescriptor, qualifier: JsExpression) {
        val name = context.getNameForDescriptor(declaration)
        statements += JsAstUtils.defineGetter(context.program, qualifier, name.ident,
                                              context.getNameForObjectInstance(declaration).makeRef())
    }

    private fun exportProperty(declaration: PropertyDescriptor, qualifier: JsExpression) {
        val propertyLiteral = JsObjectLiteral(true)

        val name = context.getNameForDescriptor(declaration).ident
        val simpleProperty = JsDescriptorUtils.isSimpleFinalProperty(declaration) &&
                             !TranslationUtils.shouldAccessViaFunctions(declaration)

        val getterBody: JsExpression = if (simpleProperty) {
            val accessToField = JsReturn(context.getInnerNameForDescriptor(declaration).makeRef())
            JsFunction(context.fragment.scope, JsBlock(accessToField), "$declaration getter")
        }
        else {
            context.getInnerNameForDescriptor(declaration.getter!!).makeRef()
        }
        propertyLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("get"), getterBody)

        if (declaration.isVar) {
            val setterBody: JsExpression = if (simpleProperty) {
                val statements = mutableListOf<JsStatement>()
                val function = JsFunction(context.fragment.scope, JsBlock(statements), "$declaration setter")
                val valueName = function.scope.declareTemporaryName("value")
                function.parameters += JsParameter(valueName)
                statements += assignment(context.getInnerNameForDescriptor(declaration).makeRef(), valueName.makeRef()).makeStmt()
                function
            }
            else {
                context.getInnerNameForDescriptor(declaration.setter!!).makeRef()
            }
            propertyLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef("set"), setterBody)
        }

        statements += JsAstUtils.defineProperty(qualifier, name, propertyLiteral, context.program).makeStmt()
    }

    private fun getLocalPackageReference(packageName: FqName): JsExpression {
        if (packageName.isRoot) {
            return context.fragment.scope.declareName(Namer.getRootPackageName()).makeRef()
        }
        var name = localPackageNames[packageName]
        if (name == null) {
            name = context.fragment.scope.declareTemporaryName("package$" + packageName.shortName().asString())
            localPackageNames.put(packageName, name)

            val parentRef = getLocalPackageReference(packageName.parent())
            val selfRef = JsNameRef(packageName.shortName().asString(), parentRef)
            val rhs = JsAstUtils.or(selfRef, assignment(selfRef.deepCopy(), JsObjectLiteral(false)))

            statements.add(JsAstUtils.newVar(name, rhs))
        }
        return name.makeRef()
    }
}

private fun MemberDescriptor.shouldBeExported(force: Boolean) =
        force || effectiveVisibility(checkPublishedApi = true).publicApi || AnnotationsUtils.getJsNameAnnotation(this) != null
