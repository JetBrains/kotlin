/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ACTUAL_WITHOUT_EXPECT") // for building kotlin-stdlib-jvm-minimal-for-test

package kotlin.text

@SinceKotlin("1.1") public actual typealias Appendable = java.lang.Appendable

@Suppress("ACTUAL_WITHOUT_EXPECT") // TODO: some supertypes are missing
@SinceKotlin("1.1") public actual typealias StringBuilder = java.lang.StringBuilder

/**
 *  The exception thrown when a character encoding or decoding error occurs.
 */
@SinceKotlin("1.4")
public actual typealias CharacterCodingException = java.nio.charset.CharacterCodingException