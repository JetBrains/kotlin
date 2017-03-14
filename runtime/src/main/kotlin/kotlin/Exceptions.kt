package kotlin

public open class Error : Throwable {

    constructor() : super() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public open class Exception : Throwable {

    constructor() : super() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public open class RuntimeException : Exception {

    constructor() : super() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public class NullPointerException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class NoSuchElementException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class IllegalArgumentException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public open class IllegalStateException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public open class UnsupportedOperationException : RuntimeException {

    constructor() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public open class IndexOutOfBoundsException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class ArrayIndexOutOfBoundsException : IndexOutOfBoundsException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class ClassCastException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class TypeCastException : ClassCastException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class ArithmeticException : RuntimeException {
    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class AssertionError : Error {

    constructor() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: Any) : super(message.toString()) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}

public open class NoWhenBranchMatchedException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public open class OutOfMemoryError : Error {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}