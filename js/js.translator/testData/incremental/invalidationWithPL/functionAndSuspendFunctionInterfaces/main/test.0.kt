import sample.*
import kotlin.coroutines.*
import kotlin.reflect.*

private fun callWithCCECheck(f: () -> Boolean): Boolean {
    return try {
        f()
    } catch (e: ClassCastException) {
        false
    }
}

// Signatures of ::target are changing - we wrap its invocations into functions.
private fun asFunction2(): Boolean =
    callWithCCECheck {
        val ret = (::target as Function2<Int, Continuation<Int>, Any?>)(41, emptyCont) as Int
        ret == 42
    }

private fun asSuspendFunction1(): Boolean =
    callWithCCECheck {
        builder {
            val ret = (::target as SuspendFunction1<Int, Int>)(41)
            ret == 42
        }
    }

private fun asFunction1(): Boolean =
    callWithCCECheck {
        val ret = (::target as Function1<Int, Int>)(41)
        ret == 42
    }

private fun asFunction3(): Boolean =
    callWithCCECheck {
        val ret = (::target as Function3<Int, Int, Continuation<Int>, Any?>)(40, 2, emptyCont) as Int
        ret == 42
    }

private fun asSuspendFunction2(): Boolean =
    callWithCCECheck {
        builder {
            val ret = (::target as SuspendFunction2<Int, Int, Int>)(40, 2)
            ret == 42
        }
    }

private fun asFunction3L(): Boolean =
    callWithCCECheck {
        val ret = (::target as Function3<Long, Int, Continuation<Int>, Any?>)(40L, 2, emptyCont) as Int
        ret == 42
    }

private fun asSuspendFunction2L(): Boolean =
    callWithCCECheck {
        builder {
            val ret = (::target as SuspendFunction2<Long, Int, Int>)(40L, 2)
            ret == 42
        }
    }

private fun asSuspendFunction3(): Boolean =
    callWithCCECheck {
        builder {
            val ret = (::target as SuspendFunction3<Long, Int, Int, Int>)(1, 2, 3)
            ret == 6
        }
    }

private fun asKSuspendFunction3(): Boolean =
    callWithCCECheck {
        builder {
            val ret = (::target as KSuspendFunction3<Long, Int, Int, Int>)(1, 2, 3)
            ret == 6
        }
    }

private fun castAsFunction4(): Boolean =
    callWithCCECheck {
        (::target as Function4<Int, Int, Int, Continuation<Int>, Any?>) != null
    }

private fun castAsSuspendFunction3(): Boolean =
    callWithCCECheck {
        (::target as SuspendFunction3<Int, Int, Int, Int>) != null
    }

private fun checks(stepId: Int, ref: Any?, check: (Boolean, String) -> Unit) {

    fun atCheck(steps: Set<Int>, pred: () -> Boolean): Boolean =
        (stepId in steps) == pred()

    val checks: List<Pair<Boolean, String>> = listOf(
        atCheck(setOf(1)) { ref is Function1<*, *> } to "ref is Function1",
        atCheck(setOf(1)) { ref is KFunction1<*, *> } to "ref is KFunction1",
        atCheck(setOf(0)) { ref is SuspendFunction1<*, *> } to "ref is SuspendFunction1",
        atCheck(setOf(0)) { ref is KSuspendFunction1<*, *> } to "ref is KSuspendFunction1",
        atCheck(setOf(0)) { ref is Function2<*, *, *> } to "ref is Function2",
        atCheck(setOf(0)) { ref is KFunction2<*, *, *> } to "ref is KFunction2",
        atCheck(setOf(2, 3)) { ref is SuspendFunction2<*, *, *> } to "ref is SuspendFunction2",
        atCheck(setOf(2, 3)) { ref is KSuspendFunction2<*, *, *> } to "ref is KSuspendFunction2",
        atCheck(setOf(2, 3)) { ref is Function3<*, *, *, *> } to "ref is Function3",
        atCheck(setOf(2, 3)) { ref is KFunction3<*, *, *, *> } to "ref is KFunction3",
        atCheck(setOf(4, 5)) { ref is SuspendFunction3<*, *, *, *> } to "ref is SuspendFunction3",
        atCheck(setOf(4, 5)) { ref is KSuspendFunction3<*, *, *, *> } to "ref is KSuspendFunction3",
        atCheck(setOf(4, 5)) { ref is Function4<*, *, *, *, *> } to "ref is Function4",
        atCheck(setOf(4, 5)) { ref is KFunction4<*, *, *, *, *> } to "ref is KFunction4",

        atCheck(setOf(1)) { asFunction1() } to "ref as Function1 call",
        atCheck(setOf(0)) { asFunction2() } to "ref as Function2 call",
        atCheck(setOf(0)) { asSuspendFunction1() } to "ref as SuspendFunction1 call",

        // KT-87511 illegal.cast instead of ClassCastException
//        atCheck(setOf(2)) { asFunction3() } to "ref as Function3 call",
//        atCheck(setOf(2)) { asSuspendFunction2() } to "ref as SuspendFunction2 call",
//        atCheck(setOf(3)) { asFunction3L() } to "ref as Function3L call",
//        atCheck(setOf(3)) { asSuspendFunction2L() } to "ref as SuspendFunction2L call",

        atCheck(setOf(4)) { stepId != 5 && asSuspendFunction3() } to "ref as SuspendFunction3 call",
        atCheck(setOf(4)) { stepId != 5 && asKSuspendFunction3() } to "ref as KSuspendFunction3 call",
        atCheck(setOf(4, 5)) { castAsFunction4() } to "ref as Function4",
        atCheck(setOf(4, 5)) { castAsSuspendFunction3() } to "ref as SuspendFunction3",
    )
    for ((result, name) in checks) {
        check(result, name)
    }
}

fun test(stepId: Int, isWasm: Boolean): String {
    if (stepId !in 0..5) return "test.kt (steps 0..5) reached unexpected step $stepId"

    val fails = StringBuilder()
    fun check(result: Boolean, msg: String) {
        if (!result) fails.append("$msg; ")
    }

    val ref = ::target
    checks(stepId, ref, ::check)

    return if (fails.isEmpty()) "OK" else "step $stepId: $fails"
}
