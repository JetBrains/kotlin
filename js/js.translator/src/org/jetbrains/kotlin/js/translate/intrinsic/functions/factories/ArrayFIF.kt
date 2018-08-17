/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import com.intellij.openapi.util.text.StringUtil.decapitalize
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.PrimitiveType.*
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.patterns.NamePredicate
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.callTranslator.CallInfo
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.BuiltInPropertyIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

object ArrayFIF : CompositeFIF() {
    @JvmField
    val GET_INTRINSIC = intrinsify { callInfo, arguments, context ->
        assert(arguments.size == 1) { "Array get expression must have one argument." }
        val (indexExpression) = arguments
        JsArrayAccess(callInfo.dispatchReceiver, indexExpression)
    }

    @JvmField
    val SET_INTRINSIC = intrinsify { callInfo, arguments, _ ->
        assert(arguments.size == 2) { "Array set expression must have two arguments." }
        val (indexExpression, value) = arguments
        val arrayAccess = JsArrayAccess(callInfo.dispatchReceiver, indexExpression)
        JsAstUtils.assignment(arrayAccess, value)
    }

    @JvmField
    val LENGTH_PROPERTY_INTRINSIC = BuiltInPropertyIntrinsic("length")

    @JvmStatic
    fun typedArraysEnabled(config: JsConfig) = config.configuration.get(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)

    fun unsignedPrimitiveToSigned(type: KotlinType): PrimitiveType? {
        // short-circuit
        if (!type.isInlineClassType() || type.isMarkedNullable) return null

        return when {
            KotlinBuiltIns.isUByte(type) -> BYTE
            KotlinBuiltIns.isUShort(type) -> SHORT
            KotlinBuiltIns.isUInt(type) -> INT
            KotlinBuiltIns.isULong(type) -> LONG
            else -> null
        }
    }

    fun castOrCreatePrimitiveArray(ctx: TranslationContext, type: KotlinType, arg: JsArrayLiteral): JsExpression {
        if (type.isMarkedNullable) return arg

        val unsignedPrimitiveType = unsignedPrimitiveToSigned(type)

        if (unsignedPrimitiveType != null) {
            val conversionFunction = "to${unsignedPrimitiveType.typeName}"
            arg.expressions.replaceAll { JsInvocation(JsNameRef(conversionFunction, it)) }
        }

        val primitiveType = unsignedPrimitiveType ?: KotlinBuiltIns.getPrimitiveType(type)?.takeUnless { type.isMarkedNullable}

        if (primitiveType == null || !typedArraysEnabled(ctx.config)) return arg

        return if (primitiveType in TYPED_ARRAY_MAP) {
            createTypedArray(primitiveType, arg)
        }
        else {
            JsAstUtils.invokeKotlinFunction(primitiveType.lowerCaseName + "ArrayOf", *arg.expressions.toTypedArray())
        }
    }

    private val TYPED_ARRAY_MAP = EnumMap(mapOf(BYTE to "Int8",
                                                SHORT to "Int16",
                                                INT to "Int32",
                                                FLOAT to "Float32",
                                                DOUBLE to "Float64"))

    private fun createTypedArray(type: PrimitiveType, arg: JsExpression): JsExpression {
        assert(type in TYPED_ARRAY_MAP)
        return JsNew(JsNameRef(TYPED_ARRAY_MAP[type] + "Array"), listOf(arg))
    }

    private val PrimitiveType.lowerCaseName
        get() = typeName.asString().toLowerCase()

    fun getTag(descriptor: CallableDescriptor, config: JsConfig): String? {
        if (descriptor !is ConstructorDescriptor) return null
        val constructedClass = descriptor.constructedClass
        if (!KotlinBuiltIns.isArrayOrPrimitiveArray(constructedClass)) return null

        if (descriptor.valueParameters.size != 2) return null
        val (sizeParam, functionParam) = descriptor.valueParameters
        if (!KotlinBuiltIns.isInt(sizeParam.type) || !functionParam.type.isBuiltinFunctionalType) return null
        if (functionParam.type.getValueParameterTypesFromFunctionType().size != 1) return null

        val primitiveType = KotlinBuiltIns.getPrimitiveArrayElementType(constructedClass.defaultType)
        return if (typedArraysEnabled(config) && primitiveType != null) {
            if (primitiveType in TYPED_ARRAY_MAP) {
                "kotlin.fillArray"
            }
            else {
                "kotlin.${primitiveType.lowerCaseName}ArrayF"
            }
        }
        else {
            if (primitiveType == CHAR) {
                "kotlin.untypedCharArrayF"
            }
            else {
                "kotlin.newArrayF"
            }
        }
    }

    init {
        val arrayName = KotlinBuiltIns.FQ_NAMES.array.shortName()

        val arrayTypeNames = mutableListOf(arrayName)
        PrimitiveType.values().mapTo(arrayTypeNames) { it.arrayTypeName }

        val arrays = NamePredicate(arrayTypeNames)
        add(pattern(arrays, "get"), GET_INTRINSIC)
        add(pattern(arrays, "set"), SET_INTRINSIC)
        add(pattern(arrays, "<get-size>"), LENGTH_PROPERTY_INTRINSIC)

        for (type in PrimitiveType.values()) {
            add(pattern(NamePredicate(type.arrayTypeName), "<init>(Int)"), intrinsify { _, arguments, context ->
                assert(arguments.size == 1) { "Array <init>(Int) expression must have one argument." }
                val (size) = arguments

                if (typedArraysEnabled(context.config)) {
                    if (type in TYPED_ARRAY_MAP) {
                        createTypedArray(type, size)
                    }
                    else {
                        JsAstUtils.invokeKotlinFunction("${type.lowerCaseName}Array", size)

                    }
                }
                else {
                    val initValue = when (type) {
                        BOOLEAN -> JsBooleanLiteral(false)
                        LONG -> JsNameRef(Namer.LONG_ZERO, Namer.kotlinLong())
                        else -> JsIntLiteral(0)
                    }
                    JsAstUtils.invokeKotlinFunction("newArray", size, initValue)
                }
            })

            add(pattern(NamePredicate(type.arrayTypeName), "<init>(Int,Function1)"), createConstructorIntrinsic(type))

            add(pattern(NamePredicate(type.arrayTypeName), "iterator"), intrinsify { callInfo, _, context ->
                val receiver = callInfo.dispatchReceiver
                if (typedArraysEnabled(context.config)) {
                    JsAstUtils.invokeKotlinFunction("${type.lowerCaseName}ArrayIterator", receiver!!)
                }
                else {
                    JsAstUtils.invokeKotlinFunction("arrayIterator", receiver!!, JsStringLiteral(type.arrayTypeName.asString()))
                }
            })
        }

        add(pattern(NamePredicate(arrayName), "<init>(Int,Function1)"), createConstructorIntrinsic(null))
        add(pattern(NamePredicate(arrayName), "iterator"), KotlinFunctionIntrinsic("arrayIterator"))

        add(pattern(Namer.KOTLIN_LOWER_NAME, "arrayOfNulls"), KotlinFunctionIntrinsic("newArray", JsNullLiteral()))

        val arrayFactoryMethodNames = arrayTypeNames.map { Name.identifier(decapitalize(it.asString() + "Of")) }
        val arrayFactoryMethods = pattern(Namer.KOTLIN_LOWER_NAME, NamePredicate(arrayFactoryMethodNames))
        add(arrayFactoryMethods, intrinsify { _, arguments, _ -> arguments[0] })
    }

    private fun createConstructorIntrinsic(type: PrimitiveType?): FunctionIntrinsic {
        return intrinsify { callInfo, arguments, context ->
            assert(arguments.size == 2) { "Array <init>(Int,Function1) expression must have two arguments." }
            val (size, fn) = arguments
            val invocation = if (typedArraysEnabled(context.config) && type != null) {
                if (type in TYPED_ARRAY_MAP) {
                    JsAstUtils.invokeKotlinFunction("fillArray", createTypedArray(type, size), fn)
                }
                else {
                    JsAstUtils.invokeKotlinFunction("${type.lowerCaseName}ArrayF", size, fn)
                }
            }
            else {
                JsAstUtils.invokeKotlinFunction(if (type == CHAR) "untypedCharArrayF" else "newArrayF", size, fn)
            }
            invocation.inlineStrategy = InlineStrategy.IN_PLACE
            val descriptor = callInfo.resolvedCall.resultingDescriptor.original
            val resolvedDescriptor = when (descriptor) {
                is TypeAliasConstructorDescriptor -> descriptor.underlyingConstructorDescriptor
                else -> descriptor
            }
            invocation.descriptor = resolvedDescriptor
            context.addInlineCall(resolvedDescriptor)
            invocation
        }
    }

    private fun intrinsify(f: (callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext) -> JsExpression)
        = object : FunctionIntrinsic() {
        override fun apply(callInfo: CallInfo, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
            return f(callInfo, arguments, context)
        }
    }
}