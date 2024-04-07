/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.sample.filePrivateSymbolsClash

annotation class A

/**
 * Defined in A.kt, clashes with B.kt
 */
@A
private fun foo() = 42

@A
private val fooProperty get() = 42
