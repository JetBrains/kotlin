/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.sample

private class PrivateClass

internal class InternalClass

class PublicClass

private fun privateFun() = 42

internal fun internalFun() = 42

fun publicFun() = 42