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


@SinceKotlin("1.1") public actual typealias Error = java.lang.Error
@SinceKotlin("1.1") public actual typealias Exception = java.lang.Exception
@SinceKotlin("1.1") public actual typealias RuntimeException = java.lang.RuntimeException
@SinceKotlin("1.1") public actual typealias IllegalArgumentException = java.lang.IllegalArgumentException
@SinceKotlin("1.1") public actual typealias IllegalStateException = java.lang.IllegalStateException
@SinceKotlin("1.1") public actual typealias IndexOutOfBoundsException = java.lang.IndexOutOfBoundsException
@SinceKotlin("1.1") public actual typealias UnsupportedOperationException = java.lang.UnsupportedOperationException

@SinceKotlin("1.1") public actual typealias NumberFormatException = java.lang.NumberFormatException
@SinceKotlin("1.1") public actual typealias NullPointerException = java.lang.NullPointerException
@SinceKotlin("1.1") public actual typealias ClassCastException = java.lang.ClassCastException
@SinceKotlin("1.1") public actual typealias AssertionError = java.lang.AssertionError

@SinceKotlin("1.1") public actual typealias NoSuchElementException = java.util.NoSuchElementException


@SinceKotlin("1.1") public actual typealias Comparator<T> = java.util.Comparator<T>
