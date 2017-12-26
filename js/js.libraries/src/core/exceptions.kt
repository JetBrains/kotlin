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

public open class Error : Throwable {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class Exception : Throwable {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class RuntimeException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class IllegalArgumentException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class IllegalStateException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class IndexOutOfBoundsException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

public open class ConcurrentModificationException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class UnsupportedOperationException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class NumberFormatException : IllegalArgumentException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

public open class NullPointerException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

public open class ClassCastException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

public open class AssertionError : Error {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: Any?) : super(message.toString(), message as? Throwable)
}

public open class NoSuchElementException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
}

public open class NoWhenBranchMatchedException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

public open class UninitializedPropertyAccessException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
