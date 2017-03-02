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
import java.util.*

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

    private val TYPED_MAP = EnumMap(mapOf(BYTE to "Int8", SHORT to "Int16", INT to "Int32", FLOAT to "Float32", DOUBLE to "Float64"))

    fun castToTypedArray(p: JsProgram, type: PrimitiveType?, arg: JsExpression): JsExpression {
        return when (type) {
            null -> arg
            in TYPED_MAP -> JsNew(JsNameRef(TYPED_MAP[type] + "Array"), listOf(arg))
            else -> JsAstUtils.invokeKotlinFunction("withType", p.getStringLiteral(type.arrayTypeName.asString()), arg)
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
        add(pattern(arrays, "iterator"), KotlinFunctionIntrinsic("arrayIterator"))

        for (type in PrimitiveType.values()) {
            add(pattern(NamePredicate(type.arrayTypeName), "<init>(Int)"), intrinsify { _, arguments, context ->
                assert(arguments.size == 1) { "Array <init>(Int) expression must have one argument." }
                val (size) = arguments
                val arg = if (type in TYPED_MAP) size else JsAstUtils.invokeKotlinFunction("newArray", size, when (type) {
                    BOOLEAN ->  JsLiteral.FALSE
                    LONG -> JsNameRef(Namer.LONG_ZERO, Namer.kotlinLong())
                    else -> JsNumberLiteral.ZERO
                })
                castToTypedArray(context.program(), type, arg)
            })

            add(pattern(NamePredicate(type.arrayTypeName), "<init>(Int,Function1)"), intrinsify { _, arguments, context ->
                assert(arguments.size == 2) { "Array <init>(Int,Function1) expression must have two arguments." }
                val (size, fn) = arguments
                castToTypedArray(context.program(), type, JsAstUtils.invokeKotlinFunction("newArrayF", size, fn))
            })
        }

        add(pattern(NamePredicate(arrayName), "<init>(Int,Function1)"), KotlinFunctionIntrinsic("newArrayF"))

        add(pattern(Namer.KOTLIN_LOWER_NAME, "arrayOfNulls"), KotlinFunctionIntrinsic("newArray", JsLiteral.NULL))

        val arrayFactoryMethodNames = arrayTypeNames.map { Name.identifier(decapitalize(it.asString() + "Of")) }
        val arrayFactoryMethods = pattern(Namer.KOTLIN_LOWER_NAME, NamePredicate(arrayFactoryMethodNames))
        add(arrayFactoryMethods, intrinsify { _, arguments, _ -> arguments[0] })
    }

    private fun intrinsify(f: (receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext) -> JsExpression)
        = object : FunctionIntrinsicWithReceiverComputed() {
            override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
                return f(receiver, arguments, context)
            }
        }
}