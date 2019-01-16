// !LANGUAGE: +ContextualEffects +UseCallsInPlaceEffect +AllowContractsForCustomFunctions
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE_ERROR
// !RENDER_DIAGNOSTICS_MESSAGES

import kotlin.contracts.*
import org.jetbrains.kotlin.contracts.contextual.*
import org.jetbrains.kotlin.contracts.contextual.exceptions.*
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.RuntimeException

inline fun myCatchIOException(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsIn(block, CatchesException<IOException>())
    }
    block()
}

class A

fun A.throwsIOException() {
    contract {
        requires(CatchesException<IOException>())
    }
    throw IOException()
}

// ---------------- TESTS ----------------

fun test_1() {
    val a = A()
    a.<!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: IOException)!>throwsIOException()<!>
}

fun test_2() {
    val a = A()
    myCatchIOException {
        a.throwsIOException()
    }
}