// KIND: STANDALONE
// MODULE: SealedCrossModuleMain(SealedCrossModuleBase)
// FILE: main.kt

// The root lives in `SealedCrossModuleBase`; this module subclasses its `open` inheritor and
// implements its leaf interface. Neither adds a case of its own.

class SubclassInMain : RecoverableError() {
    override fun toString(): String = "SubclassInMain"
}

class LeafImplInMain : LeafInterface {
    override fun toString(): String = "LeafImplInMain"
}

fun createSubclassInMain(): RootError = SubclassInMain()

fun createLeafImplInMain(): RootError = LeafImplInMain()

fun rootAsParameter(error: RootError): String = "root:$error"

// MODULE: SealedCrossModuleBase
// EXPORT_TO_SWIFT
// FILE: base.kt

sealed interface RootError

class FatalError : RootError {
    override fun toString(): String = "FatalError"
}

/** `open` and not sealed, so any module may subclass it. */
open class RecoverableError : RootError {
    override fun toString(): String = "RecoverableError"
}

/** Not sealed either, so any module may implement it. */
interface LeafInterface : RootError

class LeafImplInBase : LeafInterface {
    override fun toString(): String = "LeafImplInBase"
}

fun createFatalError(): RootError = FatalError()

fun createRecoverableError(): RootError = RecoverableError()

fun createLeafImplInBase(): RootError = LeafImplInBase()

// FILE: transport.kt

// A sealed *intermediate* branch, so the enum nests across the module boundary too. Its own
// non-exported inheritor puts `unknown` on `TransportError_SealedType` only.

sealed class TransportError : RootError

class TimeoutError : TransportError() {
    override fun toString(): String = "TimeoutError"
}

/** Not exported. */
internal class HiddenTransportError : TransportError() {
    override fun toString(): String = "HiddenTransportError"
}

fun createTimeoutError(): RootError = TimeoutError()

fun createHiddenTransportError(): RootError = HiddenTransportError()
