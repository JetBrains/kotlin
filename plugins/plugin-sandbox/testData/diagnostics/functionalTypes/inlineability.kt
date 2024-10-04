import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import org.jetbrains.kotlin.plugin.sandbox.MyNotInlineable

inline fun baseInline(f: () -> Unit) = f()
<!NOTHING_TO_INLINE!>inline<!> fun baseNoInline(noinline f: () -> Unit) = f()

inline fun myInlineable(f: @MyInlineable () -> Unit) = f()
<!NOTHING_TO_INLINE!>inline<!> fun myNotInlineable(f: @MyNotInlineable () -> Unit) = f()

inline fun testUsageNotInlineable(
    regularInline: () -> Unit,
    noinline regularNoinline: () -> Unit,
    myInline: @MyInlineable () -> Unit,
    myNoinline: @MyNotInlineable () -> Unit,
): Any? {
    return when (1) {
       1 -> <!USAGE_IS_NOT_INLINABLE!>regularInline<!>
       2 -> regularNoinline
       3 -> <!USAGE_IS_NOT_INLINABLE!>myInline<!>
       4 -> myNoinline
       else -> null
    }
}

fun testReturns(b: Boolean) {
    baseInline {
        if (b) return
    }

    baseNoInline {
        if (b) <!RETURN_NOT_ALLOWED!>return<!>
    }

    myInlineable {
        if (b) return
    }

    myNotInlineable {
        if (b) <!RETURN_NOT_ALLOWED!>return<!>
    }
}
