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

package org.jetbrains.kotlin.js.translate.intrinsic.functions.factories

import com.intellij.openapi.util.text.StringUtil.decapitalize
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.PrimitiveType.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.patterns.NamePredicate
import org.jetbrains.kotlin.js.patterns.PatternBuilder.pattern
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.BuiltInPropertyIntrinsic
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.name.Name

object ArrayFIF : CompositeFIF() {
    @JvmField
    val GET_INTRINSIC = intrinsify { receiver, arguments, _ ->
        assert(arguments.size == 1) { "Array get expression must have one argument." }
        val (indexExpression) = arguments
        JsArrayAccess(receiver!!, indexExpression)
    }

    @JvmField
    val SET_INTRINSIC = intrinsify { receiver, arguments, _ ->
        assert(arguments.size == 2) { "Array set expression must have two arguments." }
        val (indexExpression, value) = arguments
        val arrayAccess = JsArrayAccess(receiver!!, indexExpression)
        JsAstUtils.assignment(arrayAccess, value)
    }

    @JvmField
    val LENGTH_PROPERTY_INTRINSIC = BuiltInPropertyIntrinsic("length")

    public fun castToTypedArray(type: PrimitiveType?, arg: JsArrayLiteral): JsExpression {
        when (type) {
            BYTE -> "Int8"
            SHORT -> "Int16"
            INT -> "Int32"
            FLOAT -> "Float32"
            DOUBLE -> "Float64"
            else -> null
        }?.let {
            return JsNew(JsNameRef(it + "Array"), listOf(arg))
        }
        ?: return arg
    }

    init {
        val arrayTypeNames = mutableListOf(KotlinBuiltIns.FQ_NAMES.array.shortName())
        PrimitiveType.values().mapTo(arrayTypeNames) { it.arrayTypeName }

        val arrays = NamePredicate(arrayTypeNames)

        add(pattern(arrays, "get"), GET_INTRINSIC)
        add(pattern(arrays, "set"), SET_INTRINSIC)
        add(pattern(arrays, "<get-size>"), LENGTH_PROPERTY_INTRINSIC)
        add(pattern(arrays, "iterator"), KotlinFunctionIntrinsic("arrayIterator"))

        add(BOOLEAN.arrayPattern(), KotlinFunctionIntrinsic("newBooleanArray"))
        add(CHAR.arrayPattern(), KotlinFunctionIntrinsic("newCharArray"))
        add(BYTE.arrayPattern(), typedArrayIntrinsic("Int8"))
        add(SHORT.arrayPattern(), typedArrayIntrinsic("Int16"))
        add(INT.arrayPattern(), typedArrayIntrinsic("Int32"))
        add(FLOAT.arrayPattern(), typedArrayIntrinsic("Float32"))
        add(LONG.arrayPattern(), KotlinFunctionIntrinsic("newLongArray"))
        add(DOUBLE.arrayPattern(), typedArrayIntrinsic("Float64"))

        add(pattern(arrays, "<init>(Int,Function1)"), KotlinFunctionIntrinsic("newArrayF"))

        add(pattern(Namer.KOTLIN_LOWER_NAME, "arrayOfNulls"), KotlinFunctionIntrinsic("newArray", JsLiteral.NULL))

        add(BOOLEAN.arrayOfPattern(), KotlinFunctionIntrinsic("newBooleanArrayOf"))
        add(CHAR.arrayOfPattern(), KotlinFunctionIntrinsic("newCharArrayOf"))
        add(LONG.arrayOfPattern(), KotlinFunctionIntrinsic("newLongArrayOf"))

        val arrayFactoryMethodNames = arrayTypeNames.map { Name.identifier(it.toArrayOf()) }
        val arrayFactoryMethods = pattern(Namer.KOTLIN_LOWER_NAME, NamePredicate(arrayFactoryMethodNames))
        add(arrayFactoryMethods, intrinsify { _, arguments, _ -> arguments[0] })
    }

    private fun PrimitiveType.arrayPattern() = pattern(NamePredicate(arrayTypeName), "<init>(Int)")
    private fun PrimitiveType.arrayOfPattern() = pattern(Namer.KOTLIN_LOWER_NAME, arrayTypeName.toArrayOf())

    private fun Name.toArrayOf() = decapitalize(this.asString() + "Of")

    private fun typedArrayIntrinsic(typeName: String) = intrinsify { _, arguments, _ ->
        JsNew(JsNameRef(typeName + "Array"), arguments)
    }

    private fun intrinsify(f: (receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext) -> JsExpression)
        = object : FunctionIntrinsicWithReceiverComputed() {
            override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
                return f(receiver, arguments, context)
            }
        }
}
