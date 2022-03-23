/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVariance
import kotlin.reflect.typeOf

inline fun <reified T> reifiedTypeSignatureOf(): ReifiedTypeSignature<T> {
    @OptIn(UnsafeApi::class, ExperimentalStdlibApi::class)
    return ReifiedTypeSignature(reifiedTypeSignatureOf(typeOf<T>()))
}

@PublishedApi
@UnsafeApi
internal fun reifiedTypeSignatureOf(type: KType): String {
    val classifier = type.classifier ?: throw IllegalArgumentException("Expected denotable type, found $type")
    val classifierClass = classifier as? KClass<*> ?: throw IllegalArgumentException("Expected class type, found $type")
    val classifierName = classifierClass.qualifiedName ?: throw IllegalArgumentException(
        "Expected non-anonymous, non-local type, found $type"
    )

    return buildString {
        append(classifierName)
        if (type.arguments.isNotEmpty()) {
            append("<")
            type.arguments.forEachIndexed forEach@{ index, argument ->
                if (argument.type == null || argument.variance == null) {
                    append("*")
                    return@forEach
                }
                when (argument.variance) {
                    KVariance.IN -> append("in ")
                    KVariance.OUT -> append("out ")
                    else -> Unit
                }

                append(reifiedTypeSignatureOf(argument.type ?: return@forEach))
                if (index != type.arguments.lastIndex) {
                    append(", ")
                }
            }
            append(">")
        }
        if (type.isMarkedNullable) {
            append("?")
        }
    }
}

class ReifiedTypeSignature<T>
@UnsafeApi("Use 'reifiedTypeSignatureOf' instead")
@PublishedApi internal constructor(val signature: String) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReifiedTypeSignature<*>) return false
        if (other.signature != this.signature) return false
        return true
    }

    override fun hashCode(): Int {
        return 31 * signature.hashCode()
    }

    override fun toString(): String = signature

    internal companion object {
        private const val serialVersionUID = 0L
    }
}
