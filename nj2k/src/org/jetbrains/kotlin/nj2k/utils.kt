/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi

fun <T> List<T>.replace(element: T, replacer: T): List<T> {
    val mutableList = toMutableList()
    val index = indexOf(element)
    mutableList[index] = replacer
    return mutableList
}

fun String.asGetterName() =
    takeIf { JvmAbi.isGetterName(it) }
        ?.removePrefix("get")
        ?.takeIf {
            it.isNotEmpty() && it.first().isUpperCase()
                    || it.startsWith("is") && it.length > 2 && it[2].isUpperCase()
        }?.decapitalize()
        ?.escaped()

fun String.asSetterName() =
    takeIf { JvmAbi.isSetterName(it) }
        ?.removePrefix("set")
        ?.takeIf { it.isNotEmpty() && it.first().isUpperCase() }
        ?.decapitalize()
        ?.escaped()

private val KEYWORDS = KtTokens.KEYWORDS.types.map { (it as KtKeywordToken).value }.toSet()

fun String.escaped() =
    if (this in KEYWORDS || '$' in this) "`$this`"
    else this