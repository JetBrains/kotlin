/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.constants

private const val KOTLIN_PACKAGE = "kotlin"
private const val DELIMITER = "/"
private const val KOTLIN_NUMERIC_TYPE_PREFIX = "${KOTLIN_PACKAGE}${DELIMITER}"

const val BYTE = "${KOTLIN_NUMERIC_TYPE_PREFIX}Byte"
const val SHORT = "${KOTLIN_NUMERIC_TYPE_PREFIX}Short"
const val INT = "${KOTLIN_NUMERIC_TYPE_PREFIX}Int"
const val LONG = "${KOTLIN_NUMERIC_TYPE_PREFIX}Long"
const val BOOLEAN = "${KOTLIN_NUMERIC_TYPE_PREFIX}Boolean"
const val DOUBLE = "${KOTLIN_NUMERIC_TYPE_PREFIX}Double"
const val FLOAT = "${KOTLIN_NUMERIC_TYPE_PREFIX}Float"
