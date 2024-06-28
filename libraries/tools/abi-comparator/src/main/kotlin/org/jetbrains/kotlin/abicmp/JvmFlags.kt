/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp

import org.jetbrains.org.objectweb.asm.Opcodes

class AndFlag(private val flag: Int, private val name: String) : (Int) -> String? {
    override fun invoke(p1: Int): String? {
        return if (p1 and flag != 0) name else null
    }
}

class AndNotFlag(private val flag: Int, private val name: String) : (Int) -> String? {
    override fun invoke(p1: Int): String? {
        return if (p1 and flag == 0) name else null
    }
}

fun Int.isPrivate() =
    this and Opcodes.ACC_PRIVATE != 0

fun Int.isSynthetic() =
    this and Opcodes.ACC_SYNTHETIC != 0

fun Int.isBridge() =
    this and Opcodes.ACC_BRIDGE != 0

fun Int.classFlags() =
    flagsList(CLASS_FLAGS)

fun Int.methodFlags() =
    flagsList(METHOD_FLAGS)

fun Int.fieldFlags() =
    flagsList(FIELD_FLAGS)

private fun Int.flagsList(flags: List<(Int) -> String?>) =
    flags.mapNotNull { flag ->
        flag(this)
    }.joinToString(prefix = "[", postfix = "]") { it }


val CLASS_FLAGS = listOf(
    AndNotFlag(Opcodes.ACC_PUBLIC + Opcodes.ACC_PROTECTED + Opcodes.ACC_PRIVATE, "package-private"),
    AndFlag(Opcodes.ACC_PUBLIC, "public"),
    AndFlag(Opcodes.ACC_PRIVATE, "private"),
    AndFlag(Opcodes.ACC_PROTECTED, "protected"),
    AndFlag(Opcodes.ACC_STATIC, "static"),
    AndFlag(Opcodes.ACC_FINAL, "final"),
    AndFlag(Opcodes.ACC_SUPER, "super"),
    AndFlag(Opcodes.ACC_INTERFACE, "interface"),
    AndFlag(Opcodes.ACC_ABSTRACT, "abstract"),
    AndFlag(Opcodes.ACC_SYNTHETIC, "synthetic"),
    AndFlag(Opcodes.ACC_ANNOTATION, "annotation"),
    AndFlag(Opcodes.ACC_ENUM, "enum"),
    AndFlag(Opcodes.ACC_MODULE, "module)"),
    AndFlag(Opcodes.ACC_DEPRECATED, "deprecated")
)

val METHOD_FLAGS = listOf(
    AndNotFlag(Opcodes.ACC_PUBLIC + Opcodes.ACC_PROTECTED + Opcodes.ACC_PRIVATE, "package-private"),
    AndFlag(Opcodes.ACC_PUBLIC, "public"),
    AndFlag(Opcodes.ACC_PRIVATE, "private"),
    AndFlag(Opcodes.ACC_PROTECTED, "protected"),
    AndFlag(Opcodes.ACC_STATIC, "static"),
    AndFlag(Opcodes.ACC_FINAL, "final"),
    AndFlag(Opcodes.ACC_SYNCHRONIZED, "synchronized"),
    AndFlag(Opcodes.ACC_BRIDGE, "bridge"),
    AndFlag(Opcodes.ACC_VARARGS, "vararg"),
    AndFlag(Opcodes.ACC_NATIVE, "native"),
    AndFlag(Opcodes.ACC_ABSTRACT, "abstract"),
    AndFlag(Opcodes.ACC_STRICT, "strict"),
    AndFlag(Opcodes.ACC_SYNTHETIC, "synthetic"),
    AndFlag(Opcodes.ACC_DEPRECATED, "deprecated")
)

val FIELD_FLAGS = listOf(
    AndNotFlag(Opcodes.ACC_PUBLIC + Opcodes.ACC_PROTECTED + Opcodes.ACC_PRIVATE, "package-private"),
    AndFlag(Opcodes.ACC_PUBLIC, "public"),
    AndFlag(Opcodes.ACC_PRIVATE, "private"),
    AndFlag(Opcodes.ACC_PROTECTED, "protected"),
    AndFlag(Opcodes.ACC_STATIC, "static"),
    AndFlag(Opcodes.ACC_FINAL, "final"),
    AndFlag(Opcodes.ACC_VOLATILE, "volatile"),
    AndFlag(Opcodes.ACC_TRANSIENT, "transient"),
    AndFlag(Opcodes.ACC_SYNTHETIC, "synthetic"),
    AndFlag(Opcodes.ACC_ENUM, "enum"),
    AndFlag(Opcodes.ACC_DEPRECATED, "deprecated")
)