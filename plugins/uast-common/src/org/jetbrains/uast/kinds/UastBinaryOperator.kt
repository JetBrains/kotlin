/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

import org.jetbrains.uast.kinds.UastOperator

class UastBinaryOperator(override val text: String): UastOperator {
    companion object {
        @JvmField
        val ASSIGN = UastBinaryOperator("=")

        @JvmField
        val PLUS = UastBinaryOperator("+")

        @JvmField
        val MINUS = UastBinaryOperator("-")

        @JvmField
        val MULT = UastBinaryOperator("*")

        @JvmField
        val DIV = UastBinaryOperator("/")

        @JvmField
        val MOD = UastBinaryOperator("%")

        @JvmField
        val LOGICAL_OR = UastBinaryOperator("||")

        @JvmField
        val LOGICAL_AND = UastBinaryOperator("&&")

        @JvmField
        val BITWISE_OR = UastBinaryOperator("|")

        @JvmField
        val BITWISE_AND = UastBinaryOperator("&")

        @JvmField
        val BITWISE_XOR = UastBinaryOperator("^")

        @JvmField
        val EQUALS = UastBinaryOperator("==")

        @JvmField
        val NOT_EQUALS = UastBinaryOperator("!=")

        @JvmField
        val IDENTITY_EQUALS = UastBinaryOperator("===")

        @JvmField
        val IDENTITY_NOT_EQUALS = UastBinaryOperator("!==")

        @JvmField
        val GREATER = UastBinaryOperator(">")

        @JvmField
        val GREATER_OR_EQUAL = UastBinaryOperator(">=")

        @JvmField
        val LESS = UastBinaryOperator("<")

        @JvmField
        val LESS_OR_EQUAL = UastBinaryOperator("<=")

        @JvmField
        val SHIFT_LEFT = UastBinaryOperator("<<")

        @JvmField
        val SHIFT_RIGHT = UastBinaryOperator(">>")

        @JvmField
        val BITWISE_SHIFT_RIGHT = UastBinaryOperator(">>>")

        @JvmField
        val UNKNOWN = UastBinaryOperator("<unknown>")

        @JvmField
        val PLUS_ASSIGN = UastBinaryOperator("+=")

        @JvmField
        val MINUS_ASSIGN = UastBinaryOperator("-=")
        
        @JvmField
        val MULTIPLY_ASSIGN = UastBinaryOperator("*=")
        
        @JvmField
        val DIVIDE_ASSIGN = UastBinaryOperator("/=")
        
        @JvmField
        val REMAINDER_ASSIGN = UastBinaryOperator("%=")
        
        @JvmField
        val AND_ASSIGN = UastBinaryOperator("&=")
        
        @JvmField
        val XOR_ASSIGN = UastBinaryOperator("^=")
        
        @JvmField
        val OR_ASSIGN = UastBinaryOperator("|=")
        
        @JvmField
        val SHIFT_LEFT_ASSIGN = UastBinaryOperator("<<=")
        
        @JvmField
        val SHIFT_RIGHT_ASSIGN = UastBinaryOperator(">>=")
        
        @JvmField
        val BITWISE_SHIFT_RIGHT_ASSIGN = UastBinaryOperator(">>>=")
    }
}