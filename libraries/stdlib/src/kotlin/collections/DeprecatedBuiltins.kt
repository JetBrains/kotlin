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

@file:kotlin.jvm.JvmName("DeprecatedBuiltinsKt")

package kotlin

@Deprecated("Use property 'message' instead", ReplaceWith("this.message"))
public inline fun Throwable.getMessage(): String? = message

@Deprecated("Use property 'cause' instead", ReplaceWith("this.cause"))
public inline fun Throwable.getCause(): Throwable? = cause

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun Array<*>.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun ByteArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun CharArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun ShortArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun IntArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun LongArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun FloatArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun DoubleArray.size() = size

@Deprecated("Use property 'size' instead", ReplaceWith("this.size"))
public inline fun BooleanArray.size() = size
