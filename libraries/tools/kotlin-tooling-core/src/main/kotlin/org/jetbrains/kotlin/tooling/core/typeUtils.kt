/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:JvmName("TypeUtils")

package org.jetbrains.kotlin.tooling.core

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVariance

@PublishedApi
@UnsafeApi("Use 'ReifiedTypeSignature' instead")
internal fun renderReifiedTypeSignatureString(type: KType): String {
    val classifier = type.classifier ?: throw IllegalArgumentException("Expected denotable type, found $type")
    val classifierClass = classifier as? KClass<*> ?: throw IllegalArgumentException("Expected class type, found $type")
    val classifierName = classifierClass.qualifiedName ?: throw IllegalArgumentException(
        "Expected non-anonymous, non-local type, found $type"
    )

    /* Fast path: Just a non-nullable class without arguments */
    if (type.arguments.isEmpty() && !type.isMarkedNullable) {
        return classifierName
    }

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

                append(renderReifiedTypeSignatureString(argument.type ?: return@forEach))
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
