/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin

/**
 * Superclass for all platform classes representing numeric values.
 */
public abstract class Number {
    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public abstract fun toDouble(): Double

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public abstract fun toFloat(): Float

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public abstract fun toLong(): Long

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public abstract fun toInt(): Int

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public abstract fun toChar(): Char

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public abstract fun toShort(): Short

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public abstract fun toByte(): Byte
}

