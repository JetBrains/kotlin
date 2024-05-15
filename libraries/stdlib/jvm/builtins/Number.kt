/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Superclass for all platform classes representing numeric values.
 */
public actual abstract class Number {
    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public actual abstract fun toDouble(): Double

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public actual abstract fun toFloat(): Float

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public actual abstract fun toLong(): Long

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public actual abstract fun toInt(): Int

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    @Deprecated("Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\nIf you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.\nSee https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration", ReplaceWith("this.toInt().toChar()"))
    @DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.3")
    public actual open fun toChar(): Char {
        return toInt().toChar()
    }

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public actual abstract fun toShort(): Short

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public actual abstract fun toByte(): Byte
}

