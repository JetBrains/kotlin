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


// ---------------- TESTS ----------------

fun test_1() {
    <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: FileNotFoundException)!>throwsFileNotFoundException()<!>
}

fun test_2() {
    contract {
        requires(CatchesException<FileNotFoundException>())
    }
    throwsFileNotFoundException()
}

fun test_3() {
    contract {
        requires(CatchesException<IOException>())
    }
    throwsFileNotFoundException()
}

fun test_4() {
    contract {
        requires(CatchesException<NullPointerException>())
    }
    <!CONTEXTUAL_EFFECT_WARNING(Unchecked exception: FileNotFoundException)!>throwsFileNotFoundException()<!>
}