/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.annotations

/**
 * Specifies that this declaration is a part of special DSL, used for constructing function's contract.
 * That annotation can be used in compiler plugins
 */
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
annotation class ExtensionContractsDsl