/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral
import org.jetbrains.kotlin.js.backend.ast.metadata.kType
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.utils.getReferenceToJsClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

object FullyQualifiedNameFIF : FunctionIntrinsicFactory {
    override fun getIntrinsic(descriptor: FunctionDescriptor, context: TranslationContext): FunctionIntrinsic? {
        if (descriptor !is PropertyGetterDescriptor) return null
        val classDescriptor = descriptor.correspondingProperty.containingDeclaration as? ClassDescriptor ?: return null
        val type = classDescriptor.defaultType
        if (type.isMarkedNullable) return null
        if (classDescriptor.fqNameUnsafe.asString() != "kotlin.reflect.KClass") return null
        if (descriptor.correspondingProperty.name.asString() != "qualifiedName") return null

        return Intrinsic
    }

    object Intrinsic : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val resolvedCall = callInfo.resolvedCall
            val receiver = resolvedCall.dispatchReceiver ?: error("Couldn't be applied without receiver")
            val receiverValue = receiver as? ExpressionReceiver
            val classLiteralExpression = receiverValue?.expression as? KtClassLiteralExpression
            if (classLiteralExpression != null) {
                return applyToClassLiteralExpression(classLiteralExpression, context)
            }

            val receiverType = (receiver.type as? SimpleType)?.let { it.arguments.singleOrNull()?.type }

            if (receiverType != null && TypeUtils.isTypeParameter(receiverType)) {
                return generateForTypeParameter(receiverType, context)
            }

            error("Unsupported")
        }

        private fun applyToClassLiteralExpression(
            classLiteralExpression: KtClassLiteralExpression,
            context: TranslationContext
        ): JsExpression {
            val receiverExpression = classLiteralExpression.receiverExpression ?: error("Receiver expression is not found")
            val lhs = context.bindingContext().get(BindingContext.DOUBLE_COLON_LHS, receiverExpression) ?: error("LHS not found")

            if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
                error("Unsupported.")
            } else {
                val type = lhs.type

                if (TypeUtils.isTypeParameter(type)) {
                    return generateForTypeParameter(type, context)
                }

                return generateForClassLiteral(type)
            }
        }

        private fun generateForClassLiteral(type: KotlinType): JsStringLiteral {
            val name = type.constructor.declarationDescriptor?.fqNameUnsafe?.asString() ?: error("Unable to compute FQName for type $type")
            return JsStringLiteral(name)
        }

        private fun generateForTypeParameter(
            type: KotlinType,
            context: TranslationContext
        ): JsNameRef {
            assert(TypeUtils.isReifiedTypeParameter(type)) {
                "Non-reified type parameter under ::class should be rejected by type checker: $type"
            }

            val kTypeImplClass = context.currentModule.findClassAcrossModuleDependencies(
                ClassId.fromString("kotlin/reflect/js/internal/KTypeImpl")
            ) ?: error("KTypeImpl not found")

            val qualifiedNameVariable = kTypeImplClass.getMemberScope(TypeSubstitution.EMPTY)
                .getDescriptorsFiltered(DescriptorKindFilter.VARIABLES)
                .firstOrNull { it.name.identifier == "qualifiedName" }
                ?: error("qualifiedName property is not found is KTypeImpl")

            val kTypeExpression = getReferenceToJsClass(type.unwrap(), context).kType ?: error("KType not found")
            val qualifiedNameFieldName = context.getNameForDescriptor(qualifiedNameVariable)

            return JsNameRef(qualifiedNameFieldName, kTypeExpression)
        }
    }
}