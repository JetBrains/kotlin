/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package kotlin

open class Error(override val message: String?, override val cause: Throwable?) : Throwable() {
    constructor() : this(null, null)
    constructor(_message: String?) : this(_message, null)
    constructor(_cause: Throwable?) : this(null, _cause)
}

open class Exception(override val message: String?, override val cause: Throwable?) : Throwable() {
    constructor() : this(null, null)
    constructor(_message: String?) : this(_message, null)
    constructor(_cause: Throwable?) : this(null, _cause)
}

open class RuntimeException(message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}

open class IllegalArgumentException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}

open class IllegalStateException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}

open class ClassCastException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
}

open class NullPointerException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
}

open class IndexOutOfBoundsException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
}

open class AssertionError(message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor() : this(null, null)
    constructor(message: Any?) : this(message?.toString(), message as? Throwable)

}

open class UnsupportedOperationException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(null, cause)
}

open class NoSuchElementException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
}

// TODO: fix function names to satisfy style convention (depends on built-in names)
fun THROW_ISE() {
    throw IllegalStateException()
}
fun THROW_CCE() {
    throw ClassCastException()
}
fun THROW_NPE() {
    throw NullPointerException()
}
