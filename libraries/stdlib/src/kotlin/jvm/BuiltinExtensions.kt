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

public val Int.Default.MIN_VALUE: Int get() = java.lang.Integer.MIN_VALUE
public val Int.Default.MAX_VALUE: Int get() = java.lang.Integer.MAX_VALUE

public val Double.Default.MIN_VALUE: Double get() = java.lang.Double.MIN_VALUE
public val Double.Default.MAX_VALUE: Double get() = java.lang.Double.MAX_VALUE

public val Float.Default.MIN_VALUE: Float get() = java.lang.Float.MIN_VALUE
public val Float.Default.MAX_VALUE: Float get() = java.lang.Float.MAX_VALUE

public val Long.Default.MIN_VALUE: Long get() = java.lang.Long.MIN_VALUE
public val Long.Default.MAX_VALUE: Long get() = java.lang.Long.MAX_VALUE

public val Short.Default.MIN_VALUE: Short get() = java.lang.Short.MIN_VALUE
public val Short.Default.MAX_VALUE: Short get() = java.lang.Short.MAX_VALUE

public val Byte.Default.MIN_VALUE: Byte get() = java.lang.Byte.MIN_VALUE
public val Byte.Default.MAX_VALUE: Byte get() = java.lang.Byte.MAX_VALUE