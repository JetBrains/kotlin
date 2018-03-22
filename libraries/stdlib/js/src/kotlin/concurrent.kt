/*
 * Copyright 2010-2014 JetBrains s.r.o.
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


@Deprecated("Use Synchronized annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Synchronized"), level = DeprecationLevel.WARNING)
public typealias Synchronized = kotlin.jvm.Synchronized
@Deprecated("Use Volatile annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Volatile"), level = DeprecationLevel.WARNING)
public typealias Volatile = kotlin.jvm.Volatile

@kotlin.internal.InlineOnly
public inline fun <R> synchronized(@Suppress("UNUSED_PARAMETER") lock: Any, block: () -> R): R = block()
