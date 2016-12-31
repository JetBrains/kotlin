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

public class NoSuchElementException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public class IllegalArgumentException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public class IllegalStateException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public class UnsupportedOperationException : RuntimeException {

    constructor() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    constructor(cause: Throwable) : super(cause) {
    }
}

public class IndexOutOfBoundsException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public class ClassCastException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}

public class AssertionError : Error {

    constructor() {
    }

    constructor(message: String) : super(message) {
    }

    constructor(message: String, cause: Throwable) : super(message, cause) {
    }
}

public class NoWhenBranchMatchedException : RuntimeException {

    constructor() : super() {
    }

    constructor(s: String) : super(s) {
    }
}
