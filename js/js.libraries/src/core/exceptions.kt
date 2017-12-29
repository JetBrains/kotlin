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

// NOTE: Do not author your exceptions as they are written in this file, instead use this template:
/*
public open class MyException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
*/


// TODO: remove workarounds for KT-22053 from direct Throwable inheritors
// TODO: remove primary constructors, make all secondary KT-22055

@Suppress("USELESS_ELVIS_RIGHT_IS_NULL")
public open class Error(message: String?, cause: Throwable?) : Throwable(message, cause ?: null) {
    constructor() : this(null, null) {
        Error::class.js.asDynamic().call(this, null, null)
    }

    constructor(message: String?) : this(message, null) {
        Error::class.js.asDynamic().call(this, message, null)
    }

    constructor(cause: Throwable?) : this(undefined, cause) {
        Error::class.js.asDynamic().call(this, undefined, cause)
    }
}

@Suppress("USELESS_ELVIS_RIGHT_IS_NULL")
public open class Exception(message: String?, cause: Throwable?) : Throwable(message, cause ?: null) {
    constructor() : this(null, null) {
        Exception::class.js.asDynamic().call(this, null, null)
    }

    constructor(message: String?) : this(message, null) {
        Exception::class.js.asDynamic().call(this, message, null)
    }

    constructor(cause: Throwable?) : this(undefined, cause) {
        Exception::class.js.asDynamic().call(this, undefined, cause)
    }
}

public open class RuntimeException(message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}

public open class IllegalArgumentException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}

public open class IllegalStateException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}

public open class IndexOutOfBoundsException(message: String?) : RuntimeException(message) {
    constructor() : this(null)
}

public open class ConcurrentModificationException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}

public open class UnsupportedOperationException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}


public open class NumberFormatException(message: String?) : IllegalArgumentException(message) {
    constructor() : this(null)
}


public open class NullPointerException(message: String?) : RuntimeException(message) {
    constructor() : this(null)
}

public open class ClassCastException(message: String?) : RuntimeException(message) {
    constructor() : this(null)
}

public open class AssertionError private constructor(message: String?, cause: Throwable?) : Error(message, cause) {
    constructor() : this(null)
    constructor(message: String?) : this(message, null)
    constructor(message: Any?) : this(message.toString(), message as? Throwable)
}

public open class NoSuchElementException(message: String?) : RuntimeException(message) {
    constructor() : this(null)
}


public open class NoWhenBranchMatchedException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}

public open class UninitializedPropertyAccessException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(undefined, cause)
}
