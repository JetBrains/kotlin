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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.CommonClassNames.*
import com.intellij.psi.PsiField
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.j2k.ast.Expression
import org.jetbrains.kotlin.j2k.ast.Identifier
import org.jetbrains.kotlin.j2k.ast.QualifiedExpression

enum class SpecialFiled(val qualifiedClassName: String?, val fieldName: String) {

    BYTE_MAX_VALUE(JAVA_LANG_BYTE, "MAX_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.BYTE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MAX_VALUE"),
                    null)
        }
    },

    BYTE_MIN_VALUE(JAVA_LANG_BYTE, "MIN_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.BYTE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MIN_VALUE"),
                    null)
        }
    },

    
    SHORT_MAX_VALUE(JAVA_LANG_SHORT, "MAX_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.SHORT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MAX_VALUE"),
                    null)
        }
    },

    SHORT_MIN_VALUE(JAVA_LANG_SHORT, "MIN_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.SHORT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MIN_VALUE"),
                    null)
        }
    },
    
    
    INTEGER_MAX_VALUE(JAVA_LANG_INTEGER, "MAX_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.INT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MAX_VALUE"),
                    null)
        }
    },

    INTEGER_MIN_VALUE(JAVA_LANG_INTEGER, "MIN_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.INT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MIN_VALUE"),
                    null)
        }
    },


    LONG_MAX_VALUE(JAVA_LANG_LONG, "MAX_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.LONG.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MAX_VALUE"),
                    null)
        }
    },

    LONG_MIN_VALUE(JAVA_LANG_LONG, "MIN_VALUE") {
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.LONG.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MIN_VALUE"),
                    null)
        }
    },
    

    DOUBLE_MAX_VALUE(JAVA_LANG_DOUBLE, "MAX_VALUE"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.DOUBLE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MAX_VALUE"),
                    null)
        }
    },

    DOUBLE_MIN_VALUE(JAVA_LANG_DOUBLE, "MIN_VALUE"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.DOUBLE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MIN_VALUE"),
                    null)
        }
    },

    DOUBLE_POSITIVE_INFINITY(JAVA_LANG_DOUBLE, "POSITIVE_INFINITY"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.DOUBLE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("POSITIVE_INFINITY"),
                    null)
        }
    },

    DOUBLE_NEGATIVE_INFINITY(JAVA_LANG_DOUBLE, "NEGATIVE_INFINITY"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.DOUBLE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("NEGATIVE_INFINITY"),
                    null)
        }
    },

    DOUBLE_NaN(JAVA_LANG_DOUBLE, "NaN"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.DOUBLE.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("NaN"),
                    null)
        }
    },

    
    FLOAT_MAX_VALUE(JAVA_LANG_FLOAT, "MAX_VALUE"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.FLOAT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MAX_VALUE"),
                    null)
        }
    },

    FLOAT_MIN_VALUE(JAVA_LANG_FLOAT, "MIN_VALUE"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.FLOAT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("MIN_VALUE"),
                    null)
        }
    },

    FLOAT_POSITIVE_INFINITY(JAVA_LANG_FLOAT, "POSITIVE_INFINITY"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.FLOAT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("POSITIVE_INFINITY"),
                    null)
        }
    },

    FLOAT_NEGATIVE_INFINITY(JAVA_LANG_FLOAT, "NEGATIVE_INFINITY"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.FLOAT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("NEGATIVE_INFINITY"),
                    null)
        }
    },

    FLOAT_NaN(JAVA_LANG_FLOAT, "NaN"){
        override fun convertField(): Expression {
            return QualifiedExpression(
                    Identifier.withNoPrototype(PrimitiveType.FLOAT.typeName.asString(), isNullable = false),
                    Identifier.withNoPrototype("NaN"),
                    null)
        }
    };

    abstract fun convertField(): Expression

    open fun matches(field: PsiField): Boolean =
            field.name == fieldName && field.containingClass?.qualifiedName == qualifiedClassName

    companion object {
        private val valuesByName = values().groupBy { it.fieldName }

        fun match(field: PsiField): SpecialFiled? {
            val candidates = valuesByName[field.name] ?: return null
            return candidates.firstOrNull { it.matches(field) }
        }
    }
}