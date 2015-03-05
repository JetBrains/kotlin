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
package kotlin.jvm

/**
 * A constant holding the minimum value an `Int` can have, -2^31.
 */
public val Int.Default.MIN_VALUE: Int get() = java.lang.Integer.MIN_VALUE
/**
 * A constant holding the maximum value an `Int` can have, 2^31-1.
 */
public val Int.Default.MAX_VALUE: Int get() = java.lang.Integer.MAX_VALUE

/**
 * A constant holding the smallest positive nonzero value of type `Double`, 2^-1074.
 */
public val Double.Default.MIN_VALUE: Double get() = java.lang.Double.MIN_VALUE
/**
 * A constant holding the largest positive finite value of type `Double`, (2-2^-52)*2^1023.
 */
public val Double.Default.MAX_VALUE: Double get() = java.lang.Double.MAX_VALUE

/**
 * A constant holding the smallest positive nonzero value of type `Float`, 2^-149.
 */
public val Float.Default.MIN_VALUE: Float get() = java.lang.Float.MIN_VALUE
/**
 * * A constant holding the largest positive finite value of type `Float`, (2-2^-23)*2^127.
 */
public val Float.Default.MAX_VALUE: Float get() = java.lang.Float.MAX_VALUE

/**
 * A constant holding the minimum value a `Long` can have, -2^63.
 */
public val Long.Default.MIN_VALUE: Long get() = java.lang.Long.MIN_VALUE
/**
 * A constant holding the maximum value a `Long` can have, 2^63-1.
 */
public val Long.Default.MAX_VALUE: Long get() = java.lang.Long.MAX_VALUE

/**
 * A constant holding the minimum value a `Short` can have, -2^15.
 */
public val Short.Default.MIN_VALUE: Short get() = java.lang.Short.MIN_VALUE
/**
 * A constant holding the maximum value a `Short` can have, 2^15-1.
 */
public val Short.Default.MAX_VALUE: Short get() = java.lang.Short.MAX_VALUE

/**
 * A constant holding the minimum value a `Byte` can have, -128.
 */
public val Byte.Default.MIN_VALUE: Byte get() = java.lang.Byte.MIN_VALUE
/**
 * A constant holding the maximum value a `Byte` can have, 127.
 */
public val Byte.Default.MAX_VALUE: Byte get() = java.lang.Byte.MAX_VALUE