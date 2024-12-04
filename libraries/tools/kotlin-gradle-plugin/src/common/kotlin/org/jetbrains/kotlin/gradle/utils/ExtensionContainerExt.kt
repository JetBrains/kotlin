/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.plugins.ExtensionContainer

internal inline fun <reified T> ExtensionContainer.findByType(): T? = findByType(T::class.java)

internal inline fun <reified T> ExtensionContainer.getByType(): T = getByType(T::class.java)
