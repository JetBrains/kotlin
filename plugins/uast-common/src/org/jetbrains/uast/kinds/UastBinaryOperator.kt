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

/**
 * Kinds of operators in [UBinaryExpression].
 */
open class UastBinaryOperator(override val text: String): UastOperator {
    class LogicalOperator(text: String): UastBinaryOperator(text)
    class ComparisonOperator(text: String): UastBinaryOperator(text)
    class ArithmeticOperator(text: String): UastBinaryOperator(text)
    class BitwiseOperator(text: String): UastBinaryOperator(text)
    class AssignOperator(text: String): UastBinaryOperator(text)

    companion object {
        @JvmField
        val ASSIGN = AssignOperator("=")

        @JvmField
        val PLUS = ArithmeticOperator("+")

        @JvmField
        val MINUS = ArithmeticOperator("-")

        @JvmField
        val MULTIPLY = ArithmeticOperator("*")

        @JvmField
        val DIV = ArithmeticOperator("/")

        @JvmField
        val MOD = ArithmeticOperator("%")

        @JvmField
        val LOGICAL_OR = LogicalOperator("||")

        @JvmField
        val LOGICAL_AND = LogicalOperator("&&")

        @JvmField
        val BITWISE_OR = BitwiseOperator("|")

        @JvmField
        val BITWISE_AND = BitwiseOperator("&")

        @JvmField
        val BITWISE_XOR = BitwiseOperator("^")

        @JvmField
        val EQUALS = ComparisonOperator("==")

        @JvmField
        val NOT_EQUALS = ComparisonOperator("!=")

        @JvmField
        val IDENTITY_EQUALS = ComparisonOperator("===")

        @JvmField
        val IDENTITY_NOT_EQUALS = ComparisonOperator("!==")

        @JvmField
        val GREATER = ComparisonOperator(">")

        @JvmField
        val GREATER_OR_EQUAL = ComparisonOperator(">=")

        @JvmField
        val LESS = ComparisonOperator("<")

        @JvmField
        val LESS_OR_EQUAL = ComparisonOperator("<=")

        @JvmField
        val SHIFT_LEFT = BitwiseOperator("<<")

        @JvmField
        val SHIFT_RIGHT = BitwiseOperator(">>")

        @JvmField
        val UNSIGNED_SHIFT_RIGHT = BitwiseOperator(">>>")

        @JvmField
        val OTHER = UastBinaryOperator("<other>")

        @JvmField
        val PLUS_ASSIGN = AssignOperator("+=")

        @JvmField
        val MINUS_ASSIGN = AssignOperator("-=")
        
        @JvmField
        val MULTIPLY_ASSIGN = AssignOperator("*=")
        
        @JvmField
        val DIVIDE_ASSIGN = AssignOperator("/=")

        @JvmField
        val REMAINDER_ASSIGN = AssignOperator("%=")
        
        @JvmField
        val AND_ASSIGN = AssignOperator("&=")
        
        @JvmField
        val XOR_ASSIGN = AssignOperator("^=")
        
        @JvmField
        val OR_ASSIGN = AssignOperator("|=")
        
        @JvmField
        val SHIFT_LEFT_ASSIGN = AssignOperator("<<=")
        
        @JvmField
        val SHIFT_RIGHT_ASSIGN = AssignOperator(">>=")
        
        @JvmField
        val UNSIGNED_SHIFT_RIGHT_ASSIGN = AssignOperator(">>>=")
    }

    override fun toString(): String{
        return "UastBinaryOperator(text='$text')"
    }
}