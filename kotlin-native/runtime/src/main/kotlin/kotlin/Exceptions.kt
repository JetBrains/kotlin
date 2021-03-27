/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

public actual open class Error : Throwable {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class Exception : Throwable {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class RuntimeException : Exception {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class NullPointerException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}

public actual open class NoSuchElementException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}

public actual open class IllegalArgumentException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class IllegalStateException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class UnsupportedOperationException : RuntimeException {

    actual constructor()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class IndexOutOfBoundsException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}

public open class ArrayIndexOutOfBoundsException : IndexOutOfBoundsException {

    constructor() : super()

    constructor(message: String?) : super(message)
}

public actual open class ClassCastException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}

public open class TypeCastException : ClassCastException {

    constructor() : super()

    constructor(message: String?) : super(message)
}

public actual open class ArithmeticException : RuntimeException {
    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}

public actual open class AssertionError : Error {

    actual constructor()

    constructor(cause: Throwable?) : super(cause)

    actual constructor(message: Any?) : super(message?.toString())

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class NoWhenBranchMatchedException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public actual open class UninitializedPropertyAccessException : RuntimeException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)

    actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    actual constructor(cause: Throwable?) : super(cause)
}

public open class OutOfMemoryError : Error {

    constructor() : super()

    constructor(message: String?) : super(message)
}

public actual open class NumberFormatException : IllegalArgumentException {

    actual constructor() : super()

    actual constructor(message: String?) : super(message)
}

public actual open class ConcurrentModificationException actual constructor(message: String?, cause: Throwable?) :
        RuntimeException(message, cause) {

    actual constructor() : this(null, null)

    actual constructor(message: String?) : this(message, null)

    actual constructor(cause: Throwable?) : this(null, cause)
}