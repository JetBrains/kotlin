/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.util

import org.jetbrains.kaptlite.stubs.model.JavaValue
import org.jetbrains.kaptlite.stubs.model.JavaValue.*
import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.signature.SigTypeArgument
import org.jetbrains.kaptlite.stubs.StubGeneratorContext
import org.jetbrains.org.objectweb.asm.Type

sealed class JavaClassName(val packageName: String, val className: String) : Renderable {
    abstract fun getInternalName(): String

    class TopLevel(packageName: String, simpleName: String) : JavaClassName(packageName, simpleName) {
        override fun CodeScope.render() {
            if (packageName.isNotEmpty()) {
                append(packageName).append('.')
            }
            append(className)
        }

        override fun getInternalName() = if (packageName.isEmpty()) className else packageName.replace('.', '/') + '/' + className
        override fun toString() = if (packageName.isNotEmpty()) "$packageName.$className" else className
    }

    class Nested(private val outer: JavaClassName, simpleName: String) : JavaClassName(outer.packageName, simpleName) {
        override fun CodeScope.render() {
            append(outer).append('.').append(className)
        }

        override fun getInternalName() = outer.getInternalName() + "$" + className
        override fun toString() = "$outer.$className"
    }
}

fun parseType(context: StubGeneratorContext, internalName: String): SigType {
    return parseType(context, Type.getObjectType(internalName))
}

fun parseType(context: StubGeneratorContext, type: Type): SigType {
    return when (type.sort) {
        Type.ARRAY -> SigType.Array(parseType(context, type.elementType))
        Type.OBJECT -> {
            val className = context.getClassName(type.internalName)
            SigType.Class(className.toString())
        }
        else -> SigType.Primitive.get(type.descriptor.single())
    }
}

fun List<SigType>.patchTypes(context: StubGeneratorContext): List<SigType> {
    return this.map { it.patchType(context) }
}

fun SigType.patchType(context: StubGeneratorContext): SigType {
    return when (this) {
        is SigType.Primitive, is SigType.TypeVariable -> this
        is SigType.Array -> SigType.Array(elementType.patchType(context))
        is SigType.Class -> {
            val internalName = fqName.replace('.', '/')
            SigType.Class(context.getClassName(internalName).toString())
        }
        is SigType.Nested -> SigType.Nested(outer.patchType(context), name)
        is SigType.Generic -> SigType.Generic(base.patchType(context), args.map { arg ->
            when (arg) {
                SigTypeArgument.Unbound -> arg
                is SigTypeArgument.Invariant -> SigTypeArgument.Invariant(arg.type.patchType(context))
                is SigTypeArgument.Extends -> SigTypeArgument.Extends(arg.type.patchType(context))
                is SigTypeArgument.Super -> SigTypeArgument.Super(arg.type.patchType(context))
            }
        })
    }
}

val SigType.defaultValue: JavaValue
    get() = when (this) {
        is SigType.Primitive -> {
            when (this) {
                SigType.Primitive.VOID -> VNull
                SigType.Primitive.BYTE -> VByte(0)
                SigType.Primitive.SHORT -> VShort(0)
                SigType.Primitive.INT -> VInt(0)
                SigType.Primitive.LONG -> VLong(0)
                SigType.Primitive.BOOLEAN -> VBoolean(false)
                SigType.Primitive.CHAR -> VChar('\u0000')
                SigType.Primitive.FLOAT -> VFloat(0f)
                SigType.Primitive.DOUBLE -> VDouble(0.0)
                else -> error("Unknown primitive type")
            }
        }
        else -> VNull
    }

val SigType.isVoid: Boolean
    get() = this === SigType.Primitive.VOID