/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/**
 *  The exception thrown when a character encoding or decoding error occurs.
 */
@SinceKotlin("1.3")
public actual open class CharacterCodingException(message: String?) : Exception(message) {
    public actual constructor() : this(null)
}
