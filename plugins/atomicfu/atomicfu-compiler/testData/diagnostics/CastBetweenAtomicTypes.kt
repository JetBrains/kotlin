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
            { (mai as AtomicInt).value },
            { (mai as? AtomicInt)?.value },

            { (mai as AtomicLong).value },
            { (mai as? AtomicLong)?.value },

            { (mai as AtomicBoolean).value },
            { (mai as? AtomicBoolean)?.value },

            { (mai as AtomicRef<Any?>).value },
            { (mai as? AtomicRef<Any?>)?.value },

            { (mai as Int) },
            { (mai as? Int)!! },

            { (mai as AtomicIntArray)[0].value },
            { (mai as? AtomicIntArray)!![0].value },

            { (mai as AtomicLongArray)[0].value },
            { (mai as? AtomicLongArray)!![0].value },

            { (mai as AtomicBooleanArray)[0].value },
            { (mai as? AtomicBooleanArray)!![0].value },

            { (mai as AtomicArray<Any?>)[0].value },
            { (mai as? AtomicArray<Any?>)!![0].value },
        )
    }

    fun castAtomicRef() {
        val expressions = listOf(
            { (mar as AtomicInt).value },
            { (mar as? AtomicInt)?.value },

            { (mar as AtomicLong).value },
            { (mar as? AtomicLong)?.value },

            { (mar as AtomicBoolean).value },
            { (mar as? AtomicBoolean)?.value },

            { (mar as AtomicRef<Any?>).value },
            { (mar as? AtomicRef<Any?>)?.value },

            { (mar as Any) },
            { (mar as? Any)!! },

            { (mar as AtomicIntArray)[0].value },
            { (mar as? AtomicIntArray)!![0].value },

            { (mar as AtomicLongArray)[0].value },
            { (mar as? AtomicLongArray)!![0].value },

            { (mar as AtomicBooleanArray)[0].value },
            { (mar as? AtomicBooleanArray)!![0].value },

            { (mar as AtomicArray<Any?>)[0].value },
            { (mar as? AtomicArray<Any?>)!![0].value },
        )
    }

    fun castAtomicIntArray() {
        val expressions = listOf(
            { (maia as AtomicInt).value },
            { (maia as? AtomicInt)?.value },

            { (maia as AtomicLong).value },
            { (maia as? AtomicLong)?.value },

            { (maia as AtomicBoolean).value },
            { (maia as? AtomicBoolean)?.value },

            { (maia as AtomicRef<Any?>).value },
            { (maia as? AtomicRef<Any?>)?.value },

            { (maia as Any) },
            { (maia as? Any)!! },

            { (maia as AtomicIntArray)[0].value },
            { (maia as? AtomicIntArray)!![0].value },

            { (maia as AtomicLongArray)[0].value },
            { (maia as? AtomicLongArray)!![0].value },

            { (maia as AtomicBooleanArray)[0].value },
            { (maia as? AtomicBooleanArray)!![0].value },

            { (maia as AtomicArray<Any?>)[0].value },
            { (maia as? AtomicArray<Any?>)!![0].value },

            { (maia[0] as Int) },
            { (maia[0] as? Int) },

            { (maia[0] as AtomicInt).value },
            { (maia[0] as? AtomicInt)?.value },
        )
    }

    fun castAtomicArray() {
        val expressions = listOf(
            { (mara as AtomicInt).value },
            { (mara as? AtomicInt)?.value },

            { (mara as AtomicLong).value },
            { (mara as? AtomicLong)?.value },

            { (mara as AtomicBoolean).value },
            { (mara as? AtomicBoolean)?.value },

            { (mara as AtomicRef<Any?>).value },
            { (mara as? AtomicRef<Any?>)?.value },

            { (mara as Any) },
            { (mara as? Any)!! },

            { (mara as AtomicIntArray)[0].value },
            { (mara as? AtomicIntArray)!![0].value },

            { (mara as AtomicLongArray)[0].value },
            { (mara as? AtomicLongArray)!![0].value },

            { (mara as AtomicBooleanArray)[0].value },
            { (mara as? AtomicBooleanArray)!![0].value },

            { (mara as AtomicArray<String?>)[0].value },
            { (mara as? AtomicArray<String?>)!![0].value },

            { (mara[0] as Int) },
            { (mara[0] as? Int) },

            { (mara[0] as AtomicInt).value },
            { (mara[0] as? AtomicInt)?.value },

            { (mara[0] as AtomicRef<String?>).value },
            { (mara[0] as? AtomicRef<String?>)?.value },
        )
    }
    
    fun isAtomicInt() {
        val expressions = listOf(
            { mai is AtomicInt },
            { mai !is AtomicInt },
        )
    }

    fun isAtomicRef() {
        val expressions = listOf(
            { mar is Any },
            { mar !is Any },
        )
    }

    fun isAtomicIntArray() {
        val expressions = listOf(
            { maia is Any },
            { maia !is Any },

            { maia is AtomicIntArray },
            { maia !is AtomicIntArray },

            { maia[0] is AtomicInt },
            { maia[0] !is AtomicInt },
        )
    }

    fun isAtomicArray() {
        val expressions = listOf(
            { mara is Any },
            { mara !is Any },
        )
    }

    fun anyAsAtomic() {
        val expressions = listOf(
            { (any as AtomicInt).value },
            { (any as? AtomicInt)?.value },

            { any is AtomicInt },
        )
    }
}
