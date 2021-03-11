// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintLogConditionalInspection
// INSPECTION_CLASS2: com.android.tools.idea.lint.AndroidLintLogTagMismatchInspection
// INSPECTION_CLASS3: com.android.tools.idea.lint.AndroidLintLongLogTagInspection

import android.annotation.SuppressLint
import android.util.Log
import android.util.Log.DEBUG

@SuppressWarnings("UnusedDeclaration")
@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class LogTest {

    fun checkConditional(m: String) {
        Log.d(TAG1, "message") // ok: unconditional, but not performing computation
        Log.d(TAG1, m) // ok: unconditional, but not performing computation
        Log.d(TAG1, "a" + "b") // ok: unconditional, but not performing non-constant computation
        Log.d(TAG1, Constants.MY_MESSAGE) // ok: unconditional, but constant string
        <warning descr="The log call Log.i(...) should be conditional: surround with `if (Log.isLoggable(...))` or `if (BuildConfig.DEBUG) { ... }`">Log.i(TAG1, "message" + m)</warning> // error: unconditional w/ computation
        <warning descr="The log call Log.i(...) should be conditional: surround with `if (Log.isLoggable(...))` or `if (BuildConfig.DEBUG) { ... }`">Log.i(TAG1, toString())</warning> // error: unconditional w/ computation
        Log.e(TAG1, toString()) // ok: only flagging debug/info messages
        Log.w(TAG1, toString()) // ok: only flagging debug/info messages
        Log.wtf(TAG1, toString()) // ok: only flagging debug/info messages
        if (Log.isLoggable(TAG1, 0)) {
            Log.d(TAG1, toString()) // ok: conditional
        }
    }

    fun checkWrongTag(tag: String) {
        if (Log.isLoggable(<error descr="Mismatched tags: the `d()` and `isLoggable()` calls typically should pass the same tag: `TAG1` versus `TAG2` (Conflicting tag)">TAG1</error>, Log.DEBUG)) {
            Log.d(<error descr="Mismatched tags: the `d()` and `isLoggable()` calls typically should pass the same tag: `TAG1` versus `TAG2`">TAG2</error>, "message") // warn: mismatched tags!
        }
        if (Log.isLoggable("<error descr="Mismatched tags: the `d()` and `isLoggable()` calls typically should pass the same tag: `\"my_tag\"` versus `\"other_tag\"` (Conflicting tag)">my_tag</error>", Log.DEBUG)) {
            Log.d("<error descr="Mismatched tags: the `d()` and `isLoggable()` calls typically should pass the same tag: `\"my_tag\"` versus `\"other_tag\"`">other_tag</error>", "message") // warn: mismatched tags!
        }
        if (Log.isLoggable("my_tag", Log.DEBUG)) {
            Log.d("my_tag", "message") // ok: strings equal
        }
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, "message") // ok: same variable
        }
    }

    fun checkLongTag(shouldLog: Boolean) {
        if (shouldLog) {
            // String literal tags
            Log.d("short_tag", "message") // ok: short
            Log.d("<error descr="The logging tag can be at most 23 characters, was 43 (really_really_really_really_really_long_tag)">really_really_really_really_really_long_tag</error>", "message") // error: too long

            // Resolved field tags
            Log.d(TAG1, "message") // ok: short
            Log.d(TAG22, "message") // ok: short
            Log.d(TAG23, "message") // ok: threshold
            Log.d(<error descr="The logging tag can be at most 23 characters, was 24 (123456789012345678901234)">TAG24</error>, "message") // error: too long
            Log.d(<error descr="The logging tag can be at most 23 characters, was 39 (MyReallyReallyReallyReallyReallyLongTag)">LONG_TAG</error>, "message") // error: way too long

            // Locally defined variable tags
            val LOCAL_TAG = "MyReallyReallyReallyReallyReallyLongTag"
            Log.d(<error descr="The logging tag can be at most 23 characters, was 39 (MyReallyReallyReallyReallyReallyLongTag)">LOCAL_TAG</error>, "message") // error: too long

            // Concatenated tags
            Log.d(<error descr="The logging tag can be at most 23 characters, was 28 (1234567890123456789012MyTag1)">TAG22 + TAG1</error>, "message") // error: too long
            Log.d(<error descr="The logging tag can be at most 23 characters, was 27 (1234567890123456789012MyTag)">TAG22 + "MyTag"</error>, "message") // error: too long
        }
    }

    fun checkWrongLevel(tag: String) {
        if (Log.isLoggable(TAG1, Log.DEBUG)) {
            Log.d(TAG1, "message") // ok: right level
        }
        if (Log.isLoggable(TAG1, Log.INFO)) {
            Log.i(TAG1, "message") // ok: right level
        }
        if (Log.isLoggable(TAG1, <error descr="Mismatched logging levels: when checking `isLoggable` level `DEBUG`, the corresponding log call should be `Log.d`, not `Log.v` (Conflicting tag)">Log.DEBUG</error>)) {
            Log.<error descr="Mismatched logging levels: when checking `isLoggable` level `DEBUG`, the corresponding log call should be `Log.d`, not `Log.v`">v</error>(TAG1, "message") // warn: wrong level
        }
        if (Log.isLoggable(TAG1, <error descr="Mismatched logging levels: when checking `isLoggable` level `DEBUG`, the corresponding log call should be `Log.d`, not `Log.v` (Conflicting tag)">DEBUG</error>)) {
            // static import of level
            Log.<error descr="Mismatched logging levels: when checking `isLoggable` level `DEBUG`, the corresponding log call should be `Log.d`, not `Log.v`">v</error>(TAG1, "message") // warn: wrong level
        }
        if (Log.isLoggable(TAG1, <error descr="Mismatched logging levels: when checking `isLoggable` level `VERBOSE`, the corresponding log call should be `Log.v`, not `Log.d` (Conflicting tag)">Log.VERBOSE</error>)) {
            Log.<error descr="Mismatched logging levels: when checking `isLoggable` level `VERBOSE`, the corresponding log call should be `Log.v`, not `Log.d`">d</error>(TAG1, "message") // warn? verbose is a lower logging level, which includes debug
        }
        if (Log.isLoggable(TAG1, Constants.MY_LEVEL)) {
            Log.d(TAG1, "message") // ok: unknown level alias
        }
    }

    @SuppressLint("all")
    fun suppressed1() {
        Log.d(TAG1, "message") // ok: suppressed
    }

    @SuppressLint("LogConditional")
    fun suppressed2() {
        Log.d(TAG1, "message") // ok: suppressed
    }

    private object Constants {
        val MY_MESSAGE = "My Message"
        val MY_LEVEL = 5
    }

    companion object {
        private val TAG1 = "MyTag1"
        private val TAG2 = "MyTag2"
        private val TAG22 = "1234567890123456789012"
        private val TAG23 = "12345678901234567890123"
        private val TAG24 = "123456789012345678901234"
        private val LONG_TAG = "MyReallyReallyReallyReallyReallyLongTag"
    }
}