/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

private interface Source {
    fun get(): String
}

private class O : Source {
    override fun get(): String = "O"
}

private class K : Source {
    override fun get(): String = "K"
}

@NoInline
@Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD")
private fun render(source: Source): String = run {
    while (true) {
        return@run source.get()
    }
    error("unreachable")
}.toString() // The identity intrinsic reuses the virtual call's DFG node outside the loop scope.

fun box(): String = render(O()) + render(K())
