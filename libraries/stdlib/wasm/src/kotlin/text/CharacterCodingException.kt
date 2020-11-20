/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 *  The exception thrown when a character encoding or decoding error occurs.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual open class CharacterCodingException actual constructor() : Exception()