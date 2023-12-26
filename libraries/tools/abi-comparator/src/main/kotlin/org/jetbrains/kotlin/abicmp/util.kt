/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp

inline fun <reified T : Any> List<Any?>?.listOfNotNull() = orEmpty().filterIsInstance<T>()

const val PROPERTY_VAL_STUB = "---"