// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.exceptions.*
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.RuntimeException

fun throwsFileNotFoundException() {
    contract {
        requires(CatchesException<FileNotFoundException>())
    }
    throw FileNotFoundException()
}

fun throwsNullPointerException() {
    contract {
        requires(CatchesException<NullPointerException>())
    }
    throw NullPointerException()
}

fun throwsIOException() {
    contract {
        requires(CatchesException<IOException>())
    }
    throw java.io.IOException()
}

inline fun myCatchIOException(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsIn(block, CatchesException<IOException>())
    }
    block()
}

inline fun myCatchRuntimeException(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsIn(block, CatchesException<RuntimeException>())
    }
    block()
}

// ---------------- TESTS ----------------

fun test_1() {
    myCatchRuntimeException {
        <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: IOException)!>throwsIOException()<!>
    }
}

fun test_2() {
    myCatchIOException {
        myCatchRuntimeException {
            throwsIOException()
        }
    }
}

fun test_3() {
    myCatchIOException {
        <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: NullPointerException)!>throwsNullPointerException()<!>
        myCatchRuntimeException {}
    }
}

fun test_4() {
    myCatchIOException {
        myCatchRuntimeException {}
        <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: NullPointerException)!>throwsNullPointerException()<!>
    }
}

fun test_5() {
    myCatchIOException {
        throwsIOException()
        myCatchRuntimeException {}
    }
}

fun test_6() {
    myCatchIOException {
        myCatchRuntimeException {}
        throwsIOException()
    }
}