/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SpecialFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.kTypeWithRecursion
import org.jetbrains.kotlin.js.backend.ast.metadata.specialFunction
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.utils.getReferenceToJsClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.*

object TypeOfFIF : FunctionIntrinsicFactory {
    override fun getIntrinsic(descriptor: FunctionDescriptor, context: TranslationContext): FunctionIntrinsic? =
        Intrinsic.takeIf { descriptor.fqNameUnsafe.asString() == "kotlin.reflect.typeOf" }

    object Intrinsic : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            val type = callInfo.resolvedCall.typeArguments.values.single()
            return context.createKType(type)
        }
    }
}

fun TranslationContext.createKType(type: KotlinType): JsExpression = KTypeConstructor(this).createKType(type)

private class KTypeConstructor(private val context: TranslationContext) {
    private val visitedTypeParams = hashSetOf<TypeParameterDescriptor>()

    fun callHelperFunction(name: String, vararg arguments: JsExpression) =
        JsInvocation(context.getReferenceToIntrinsic(name), *arguments)

    fun createKType(type: KotlinType): JsExpression {
        val unwrappedType = type.unwrap()
        if (unwrappedType is SimpleType)
            return createSimpleKType(unwrappedType)
        if (unwrappedType is DynamicType)
            return createDynamicType()
        error("Unexpected type $type")
    }

    private fun createDynamicType(): JsExpression {
        return callHelperFunction(Namer.CREATE_DYNAMIC_KTYPE)
    }

    private fun createSimpleKType(type: SimpleType): JsExpression {
        val typeConstructor = type.constructor
        val classifier = typeConstructor.declarationDescriptor

        if (classifier is TypeParameterDescriptor && classifier.isReified) {
            val kClassName = context.getNameForIntrinsic(SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE.suggestedName)
            kClassName.specialFunction = SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE

            val reifiedTypeParameterType = JsInvocation(kClassName.makeRef(), getReferenceToJsClass(classifier, context))
            if (type.isMarkedNullable) {
                return callHelperFunction(Namer.MARK_KTYPE_NULLABLE, reifiedTypeParameterType)
            }

            return reifiedTypeParameterType
        }

        val kClassifier =
            when {
                classifier != null -> {
                    createKClassifier(classifier)
                }
                typeConstructor is IntersectionTypeConstructor -> {
                    val getKClassM = context.getNameForIntrinsic("getKClassM")
                    val args = JsArrayLiteral(
                        typeConstructor.supertypes.map { getReferenceToJsClass(it.constructor.declarationDescriptor, context) }
                    )

                    JsInvocation(getKClassM.makeRef(), args)
                }
                else -> {
                    error("Can't get KClass for $type")
                }
            }
        val arguments = JsArrayLiteral(type.arguments.map { createKTypeProjection(it) })
        val isMarkedNullable = JsBooleanLiteral(type.isMarkedNullable)
        return callHelperFunction(
            Namer.CREATE_KTYPE,
            kClassifier,
            arguments,
            isMarkedNullable
        )
    }

    private fun createKTypeProjection(tp: TypeProjection): JsExpression {
        if (tp.isStarProjection) {
            return callHelperFunction(Namer.GET_START_KTYPE_PROJECTION)
        }

        val factoryName = when (tp.projectionKind) {
            Variance.INVARIANT -> Namer.CREATE_INVARIANT_KTYPE_PROJECTION
            Variance.IN_VARIANCE -> Namer.CREATE_CONTRAVARIANT_KTYPE_PROJECTION
            Variance.OUT_VARIANCE -> Namer.CREATE_COVARIANT_KTYPE_PROJECTION
        }

        val kType = createKType(tp.type)
        return callHelperFunction(factoryName, kType)

    }

    private fun createKClassifier(classifier: ClassifierDescriptor): JsExpression =
        when (classifier) {
            is TypeParameterDescriptor -> createKTypeParameter(classifier, visitedTypeParams)
            else -> ExpressionVisitor.getObjectKClass(context, classifier)
        }

    private fun createKTypeParameter(
        typeParameter: TypeParameterDescriptor,
        visitedTypeParams: MutableSet<TypeParameterDescriptor>
    ): JsExpression {
        fun createKTypeParameterImpl(): JsExpression {
            val name = JsStringLiteral(typeParameter.name.asString())
            val upperBounds = JsArrayLiteral(typeParameter.upperBounds.map { createKType(it) })
            val variance = when (typeParameter.variance) {
                //// TODO use consts, enums, numbers???
                Variance.INVARIANT -> JsStringLiteral("invariant")
                Variance.IN_VARIANCE -> JsStringLiteral("in")
                Variance.OUT_VARIANCE -> JsStringLiteral("out")
            }
            if (typeParameter.isReified) {
                val kClassName = context.getNameForIntrinsic(SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE.suggestedName)
                kClassName.specialFunction = SpecialFunction.GET_REIFIED_TYPE_PARAMETER_KTYPE

                return JsInvocation(kClassName.makeRef(), getReferenceToJsClass(typeParameter, context))
            }

            return callHelperFunction(
                Namer.CREATE_KTYPE_PARAMETER,
                name,
                upperBounds,
                variance
            )
        }

        if (typeParameter in visitedTypeParams) {
            return callHelperFunction(Namer.GET_START_KTYPE_PROJECTION).also {
                it.kTypeWithRecursion = true
            }
        }

        visitedTypeParams.add(typeParameter)

        return createKTypeParameterImpl().also {
            visitedTypeParams.remove(typeParameter)
        }
    }
}
