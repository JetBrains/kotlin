// CORRECT_ERROR_TYPES
// EXPECTED_ERROR(11;5) cannot find symbol (Kotlin location: /maxErrorCount.kt: (9, 5))
// JAVAC_OPTION -Xmaxerrs=1

@file:Suppress("UNRESOLVED_REFERENCE")
import kotlin.reflect.KClass

class Test {
    fun a(a: ABC, b: BCD) {}
}

// There are two errors (unresolved identifier ABC, BCD) actually.
// But we specified the max error count, so the error output is limited.