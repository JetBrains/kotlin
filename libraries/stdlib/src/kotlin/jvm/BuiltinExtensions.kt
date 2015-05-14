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
 * A constant holding the smallest positive nonzero value of type `Double`, 2^-1074.
 */
public val Double.Companion.MIN_VALUE: Double get() = java.lang.Double.MIN_VALUE
/**
 * A constant holding the largest positive finite value of type `Double`, (2-2^-52)*2^1023.
 */
public val Double.Companion.MAX_VALUE: Double get() = java.lang.Double.MAX_VALUE

/**
 * A constant holding the smallest positive nonzero value of type `Float`, 2^-149.
 */
public val Float.Companion.MIN_VALUE: Float get() = java.lang.Float.MIN_VALUE
/**
 * * A constant holding the largest positive finite value of type `Float`, (2-2^-23)*2^127.
 */
public val Float.Companion.MAX_VALUE: Float get() = java.lang.Float.MAX_VALUE
