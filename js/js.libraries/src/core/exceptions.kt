/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

public open class Error(message: String? = null) : Throwable(message, null)

public open class Exception(message: String? = null) : Throwable(message, null)

public open class RuntimeException(message: String? = null) : Exception(message)

public open class IllegalArgumentException(message: String? = null) : RuntimeException(message)

public open class IllegalStateException(message: String? = null) : RuntimeException(message)

public open class IndexOutOfBoundsException(message: String? = null) : RuntimeException(message)

public open class ConcurrentModificationException(message: String? = null) : RuntimeException(message)

public open class UnsupportedOperationException(message: String? = null) : RuntimeException(message)

public open class NumberFormatException(message: String? = null) : RuntimeException(message)

public open class NullPointerException(message: String? = null) : RuntimeException(message)

public open class ClassCastException(message: String? = null) : RuntimeException(message)

public open class AssertionError(message: String? = null) : Error(message)

public open class NoSuchElementException(message: String? = null) : Exception(message)

public open class NoWhenBranchMatchedException(message: String? = null) : RuntimeException(message)
