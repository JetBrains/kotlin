// KIND: STANDALONE
// MODULE: SealedInheritorKinds
// FILE: object_inheritor.kt

// A singleton `object` as a direct inheritor: it gets an ordinary case whose payload carries the
// singleton. Identity is checked on the Kotlin side: every Swift access builds a wrapper (KT-48137).

sealed interface AppError

sealed class IoError : AppError

class FileReadError(val file: String) : IoError() {
    override fun toString(): String = "FileReadError($file)"
}

class DatabaseError(val source: String) : IoError() {
    override fun toString(): String = "DatabaseError($source)"
}

object RuntimeFailure : AppError {
    override fun toString(): String = "RuntimeFailure"
}

fun createRuntimeFailure(): AppError = RuntimeFailure

fun createFileReadError(): AppError = FileReadError("example.txt")

fun isRuntimeFailureSingleton(value: AppError): Boolean = value === RuntimeFailure

// FILE: value_class_inheritor.kt

// A `value class` inheritor has no scope-defining Swift declaration, so its case is generated from
// the boxed class.

sealed interface ValueHost

value class WrappedInt(val raw: Int) : ValueHost

class PlainHost : ValueHost {
    override fun toString(): String = "PlainHost"
}

fun createWrappedInt(): ValueHost = WrappedInt(7)

fun createPlainHost(): ValueHost = PlainHost()

// FILE: fun_interface_inheritor.kt

// A `fun interface` inheritor reached through an exported class and through a SAM-converted lambda,
// whose runtime class has no generated Swift counterpart. Both land on its case (KT-88999).

sealed interface CallbackHost

fun interface EventCallback : CallbackHost {
    fun onEvent(): String
}

class PlainCallbackHost : CallbackHost {
    override fun toString(): String = "PlainCallbackHost"
}

class EventCallbackImpl : EventCallback {
    override fun onEvent(): String = "from exported class"
}

fun createSamCallback(): CallbackHost = EventCallback { "from SAM lambda" }

fun createCallbackImpl(): CallbackHost = EventCallbackImpl()

fun createPlainCallbackHost(): CallbackHost = PlainCallbackHost()

// FILE: constructors.kt

// Non-public root constructors with public leaf constructors: values passed to the root stay
// reachable through the enum payload. `DiskError` reaches its *private* primary constructor
// through the protected secondary one a leaf can call.

sealed class Failure(val message: String) {
    /** Explicitly protected secondary constructor, used by a leaf below. */
    protected constructor(code: Int) : this("code=$code")
}

class NotFound : Failure("not found")

class Timeout(seconds: Int) : Failure(seconds)

sealed class DiskError private constructor(val label: String) {
    constructor() : this("via-private-ctor")
}

class DiskFullError : DiskError()

fun createTimeout(): Failure = Timeout(30)

fun createDiskFullError(): DiskError = DiskFullError()

// FILE: generic_class.kt

// A generic sealed *class* still gets a full enum (unlike a generic sealed interface, KT-87798).
// `QueryValue.value` deliberately clashes with the generated leaf struct's own payload name.

sealed class Query<out T>

class QueryValue<T>(val value: T) : Query<T>()

object QueryLoading : Query<Nothing>()

class QueryFailure(val message: String) : Query<Nothing>()

fun createQueryLoading(): Query<String> = QueryLoading

fun createQueryValue(): Query<String> = QueryValue("payload")

fun createQueryFailure(): Query<String> = QueryFailure("boom")
