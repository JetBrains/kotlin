/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.util

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.EnumSet
import javax.lang.model.element.Modifier

fun Int.test(flag: Int): Boolean {
    return (this and flag) > 0
}

val ClassNode.isPublic: Boolean get() = access.test(Opcodes.ACC_PUBLIC)
val ClassNode.isFinal: Boolean get() = access.test(Opcodes.ACC_FINAL)
val ClassNode.isSynthetic: Boolean get() = access.test(Opcodes.ACC_SYNTHETIC)
val ClassNode.isEnum: Boolean get() = access.test(Opcodes.ACC_ENUM)

val FieldNode.isSynthetic: Boolean get() = access.test(Opcodes.ACC_SYNTHETIC)
val FieldNode.isStatic: Boolean get() = access.test(Opcodes.ACC_STATIC)
val FieldNode.isEnumValue: Boolean get() = access.test(Opcodes.ACC_ENUM)

val MethodNode.isSynthetic: Boolean get() = access.test(Opcodes.ACC_SYNTHETIC)
val MethodNode.isBridge: Boolean get() = access.test(Opcodes.ACC_BRIDGE)
val MethodNode.isVarargs: Boolean get() = access.test(Opcodes.ACC_VARARGS)
val MethodNode.isAbstract: Boolean get() = access.test(Opcodes.ACC_ABSTRACT)
val MethodNode.isStaticInitializer: Boolean get() = name == "<clinit>"
val MethodNode.isConstructor: Boolean get() = name == "<init>"

private fun parseVisibilityModifiers(access: Int, consumer: EnumSet<Modifier>) {
    if (access.test(Opcodes.ACC_PUBLIC)) consumer.add(Modifier.PUBLIC)
    if (access.test(Opcodes.ACC_PROTECTED)) consumer.add(Modifier.PROTECTED)
    if (access.test(Opcodes.ACC_PRIVATE)) consumer.add(Modifier.PRIVATE)
}

private fun parseModalityModifiers(access: Int, consumer: EnumSet<Modifier>) {
    val isEnum = access.test(Opcodes.ACC_ENUM)
    if (access.test(Opcodes.ACC_FINAL) && !isEnum) consumer.add(Modifier.FINAL)
    if (access.test(Opcodes.ACC_ABSTRACT) && !isEnum) consumer.add(Modifier.ABSTRACT)
}

fun parseModifiers(node: ClassNode): Set<Modifier> {
    val access = node.access
    val modifiers = EnumSet.noneOf(Modifier::class.java)
    parseVisibilityModifiers(access, modifiers)
    parseModalityModifiers(access, modifiers)
    return modifiers
}

fun parseModifiers(node: MethodNode): Set<Modifier> {
    val access = node.access
    val modifiers = EnumSet.noneOf(Modifier::class.java)
    parseVisibilityModifiers(access, modifiers)
    parseModalityModifiers(access, modifiers)
    if (access.test(Opcodes.ACC_SYNCHRONIZED)) modifiers.add(Modifier.SYNCHRONIZED)
    if (access.test(Opcodes.ACC_NATIVE)) modifiers.add(Modifier.NATIVE)
    if (access.test(Opcodes.ACC_STRICT)) modifiers.add(Modifier.STRICTFP)
    if (access.test(Opcodes.ACC_STATIC)) modifiers.add(Modifier.STATIC)
    return modifiers
}

fun parseModifiers(node: FieldNode): Set<Modifier> {
    val access = node.access
    val modifiers = EnumSet.noneOf(Modifier::class.java)
    parseVisibilityModifiers(access, modifiers)
    parseModalityModifiers(access, modifiers)
    if (access.test(Opcodes.ACC_VOLATILE)) modifiers.add(Modifier.VOLATILE)
    if (access.test(Opcodes.ACC_TRANSIENT)) modifiers.add(Modifier.TRANSIENT)
    if (access.test(Opcodes.ACC_STATIC)) modifiers.add(Modifier.STATIC)
    return modifiers
}