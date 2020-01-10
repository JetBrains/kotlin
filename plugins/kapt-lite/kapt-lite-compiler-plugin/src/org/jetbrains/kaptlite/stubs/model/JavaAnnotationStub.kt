/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.model

import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.stubs.StubGeneratorContext
import org.jetbrains.kaptlite.stubs.util.*
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.Type

class JavaAnnotationStub(val name: JavaClassName, val values: List<JavaAnnotationValue>) : Renderable {
    companion object {
        fun parse(
            context: StubGeneratorContext,
            access: Int,
            visibleAnnotations: List<AnnotationNode>?,
            invisibleAnnotations: List<AnnotationNode>?
        ): List<JavaAnnotationStub> {
            val visible = visibleAnnotations ?: emptyList()
            val invisible = invisibleAnnotations ?: emptyList()

            val annotations = ArrayList<JavaAnnotationStub>(visible.size + invisible.size)
            visible.forEach { annotations += parse(context, it) }
            invisible.forEach { annotations += parse(context, it) }

            if (access.test(Opcodes.ACC_DEPRECATED)) {
                if (annotations.none { it.name.packageName == "java.lang" && it.name.className == "Deprecated" }) {
                    annotations += JavaAnnotationStub(JavaClassName.TopLevel("java.lang", "Deprecated"), emptyList())
                }
            }

            return annotations
        }

        fun parse(context: StubGeneratorContext, node: AnnotationNode): JavaAnnotationStub {
            val internalName = Type.getType(node.desc).internalName
            val name = context.getClassName(internalName)
            val values = parseValues(context, node.values ?: emptyList())
            return JavaAnnotationStub(name, values)
        }

        private fun parseValues(context: StubGeneratorContext, values: List<Any?>): List<JavaAnnotationValue> {
            val result = ArrayList<JavaAnnotationValue>(values.size / 2)
            for (index in 0 until values.size / 2) {
                val name = values[index * 2] as? String ?: continue
                val value = JavaValue.parse(context, values[index * 2 + 1])
                result += JavaAnnotationValue(name, value)
            }
            return result
        }
    }

    override fun CodeScope.render() {
        append('@')
        append(name)
        appendList(values, prefix = "(", postfix = ")")
    }
}

class JavaAnnotationValue(val name: String, val value: JavaValue) : Renderable {
    override fun CodeScope.render() {
        append(name).append(" = ")
        append(value)
    }
}

sealed class JavaValue : Renderable {
    companion object {
        fun parse(context: StubGeneratorContext, value: Any?): JavaValue {
            return when (value) {
                is Byte -> VByte(value)
                is Boolean -> VBoolean(value)
                is Char -> VChar(value)
                is Short -> VShort(value)
                is Int -> VInt(value)
                is Long -> VLong(value)
                is Float -> VFloat(value)
                is Double -> VDouble(value)
                is String -> VString(value)
                is Type -> VType(parseType(context, value))
                is Array<*> -> {
                    if (value.size != 2 || value[0] !is String || value[1] !is String) {
                        error("Unexpected enum value: " + value.contentDeepToString())
                    }

                    val enum = context.getClassName(Type.getType(value[0] as String).internalName)
                    VEnumEntry(enum, value[1] as String)
                }
                is AnnotationNode -> return VAnnotation(JavaAnnotationStub.parse(context, value))
                is List<*> -> VArray(value.map { parse(context, it) })
                else -> error("Unexpected value type $value")
            }
        }
    }

    class VByte(private val value: Byte) : JavaValue() {
        override fun CodeScope.render() {
            append(value.toString())
        }
    }

    class VBoolean(private val value: Boolean) : JavaValue() {
        override fun CodeScope.render() {
            append(if (value) "true" else "false")
        }
    }

    class VChar(private val value: Char) : JavaValue() {
        override fun CodeScope.render() {
            append("'${escape(value)}'")
        }
    }

    class VShort(private val value: Short) : JavaValue() {
        override fun CodeScope.render() {
            append(value.toString())
        }
    }

    class VInt(private val value: Int) : JavaValue() {
        override fun CodeScope.render() {
            append(value.toString())
        }
    }

    class VLong(private val value: Long) : JavaValue() {
        override fun CodeScope.render() {
            append(value.toString()).append('L')
        }
    }

    class VFloat(private val value: Float) : JavaValue() {
        override fun CodeScope.render() {
            when {
                value.isNaN() -> append("java.lang.Float.NaN")
                value == Float.POSITIVE_INFINITY -> append("java.lang.Float.POSITIVE_INFINITY")
                value == Float.NEGATIVE_INFINITY -> append("java.lang.Float.NEGATIVE_INFINITY")
                else -> append(value.toString()).append('f')
            }
        }
    }

    class VDouble(private val value: Double) : JavaValue() {
        override fun CodeScope.render() {
            when {
                value.isNaN() -> append("java.lang.Double.NaN")
                value == Double.POSITIVE_INFINITY -> append("java.lang.Double.POSITIVE_INFINITY")
                value == Double.NEGATIVE_INFINITY -> append("java.lang.Double.NEGATIVE_INFINITY")
                else -> append(value.toString())
            }
        }
    }

    class VString(private val value: String) : JavaValue() {
        override fun CodeScope.render() {
            val escaped = buildString {
                for (char in value) {
                    append(escape(char))
                }
            }
            append("\"$escaped\"")
        }
    }

    class VType(private val type: SigType) : JavaValue() {
        override fun CodeScope.render() {
            append(type).append(".class")
        }
    }

    class VEnumEntry(private val enum: JavaClassName, private val value: String) : JavaValue() {
        override fun CodeScope.render() {
            append(enum).append('.').append(value)
        }
    }

    class VAnnotation(private val value: JavaAnnotationStub) : JavaValue() {
        override fun CodeScope.render() {
            append(value)
        }
    }

    class VArray(private val values: List<JavaValue>) : JavaValue() {
        override fun CodeScope.render() {
            append('{').appendList(values).append('}')
        }
    }

    class VExpressionUnsafe(private val value: String) : JavaValue() {
        override fun CodeScope.render() {
            append(value)
        }
    }

    object VNull : JavaValue() {
        override fun CodeScope.render() {
            append("null")
        }
    }
}

private fun escape(char: Char): String {
    return when (char) {
        '\t' -> "\\t"
        '\b' -> "\\b"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\u000c' -> "\\f"
        '\'' -> "\\'"
        '"' -> "\\\""
        '\\' -> "\\\\"
        in ' '..'~' -> "$char"
        else -> String.format("\\u%04x", char.toInt())
    }
}