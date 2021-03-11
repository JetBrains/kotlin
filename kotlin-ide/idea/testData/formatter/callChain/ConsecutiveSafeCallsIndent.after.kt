class Some {
    fun some(): Some? = this
}

public fun bar(): String? =
        Some()
                ?.some()
                ?.some()
                ?.some()!!
                .toString()

// SET_TRUE: CONTINUATION_INDENT_FOR_CHAINED_CALLS
