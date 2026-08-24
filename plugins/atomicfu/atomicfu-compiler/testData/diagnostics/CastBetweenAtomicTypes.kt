// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

// Casting AFU's properties to other types is, in general, prohibited.
// The only supported case is casting AtomicRef<T> to AtomicRef<R> where T and R are some types

@Suppress("USELESS_CAST", "CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST", "USELESS_IS_CHECK")
class C {
    private val mai: AtomicInt = atomic(0)
    private val mar: AtomicRef<Any> = atomic("")

    private val maia: AtomicIntArray = AtomicIntArray(1)
    private val mara: AtomicArray<Any?> = atomicArrayOfNulls(1)

    private val any: Any = ""

    fun castAtomicInt() {
        val expressions = listOf(
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicInt<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicLong<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicLong<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicBoolean<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicBoolean<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicRef<Any?><!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicRef<Any?><!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as Int<!>) },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? Int<!>)!! },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicIntArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicIntArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicLongArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicLongArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicBooleanArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicBooleanArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as AtomicArray<Any?><!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai as? AtomicArray<Any?><!>)!![0].value },
        )
    }

    fun castAtomicRef() {
        val expressions = listOf(
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicInt<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicLong<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicLong<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicBoolean<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicBoolean<!>)?.value },

            { (mar as AtomicRef<Any?>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicRef<Any?><!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as Any<!>) },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? Any<!>)!! },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicIntArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicIntArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicLongArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicLongArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicBooleanArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicBooleanArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as AtomicArray<Any?><!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar as? AtomicArray<Any?><!>)!![0].value },
        )
    }

    fun castAtomicIntArray() {
        val expressions = listOf(
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicInt<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicLong<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicLong<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicBoolean<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicBoolean<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicRef<Any?><!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicRef<Any?><!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as Any<!>) },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? Any<!>)!! },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicIntArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicIntArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicLongArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicLongArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicBooleanArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicBooleanArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as AtomicArray<Any?><!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia as? AtomicArray<Any?><!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia[0] as Int<!>) },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia[0] as? Int<!>) },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia[0] as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia[0] as? AtomicInt<!>)?.value },
        )
    }

    fun castAtomicArray() {
        val expressions = listOf(
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicInt<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicLong<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicLong<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicBoolean<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicBoolean<!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicRef<Any?><!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicRef<Any?><!>)?.value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as Any<!>) },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? Any<!>)!! },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicIntArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicIntArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicLongArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicLongArray<!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as AtomicBooleanArray<!>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicBooleanArray<!>)!![0].value },

            { (mara as AtomicArray<String?>)[0].value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara as? AtomicArray<String?><!>)!![0].value },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara[0] as Int<!>) },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara[0] as? Int<!>) },

            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara[0] as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara[0] as? AtomicInt<!>)?.value },

            { (mara[0] as AtomicRef<String?>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara[0] as? AtomicRef<String?><!>)?.value },
        )
    }
    
    fun isAtomicInt() {
        val expressions = listOf(
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai is AtomicInt<!> },
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mai !is AtomicInt<!> },
        )
    }

    fun isAtomicRef() {
        val expressions = listOf(
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar is Any<!> },
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mar !is Any<!> },
        )
    }

    fun isAtomicIntArray() {
        val expressions = listOf(
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia is Any<!> },
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia !is Any<!> },

            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia is AtomicIntArray<!> },
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia !is AtomicIntArray<!> },

            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia[0] is AtomicInt<!> },
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>maia[0] !is AtomicInt<!> },
        )
    }

    fun isAtomicArray() {
        val expressions = listOf(
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara is Any<!> },
            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>mara !is Any<!> },
        )
    }

    fun anyAsAtomic() {
        val expressions = listOf(
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>any as AtomicInt<!>).value },
            { (<!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>any as? AtomicInt<!>)?.value },

            { <!ATOMIC_TYPE_OPERATOR_IS_FORBIDDEN!>any is AtomicInt<!> },
        )
    }


}
