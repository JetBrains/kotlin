// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR -UNUSED_VARIABLE
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
    val x: Int = 10
    when (x) {
        in 0..3 -> myCatchIOException {
            throwsIOException()
        }
        in 5..7 -> myCatchRuntimeException {
            <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: IOException)!>throwsIOException()<!>
        }
        else -> <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: NullPointerException)!>throwsNullPointerException()<!>
    }
}

fun test_2() {
    val x: Int = 10
    <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: NullPointerException)!>throwsNullPointerException()<!>
    when (x) {
        in 0..3 -> myCatchIOException {
            throwsFileNotFoundException()
        }
        in 5..7 -> myCatchRuntimeException {
            <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: IOException)!>throwsIOException()<!>
        }
        else -> {
            val y = x
        }
    }
}