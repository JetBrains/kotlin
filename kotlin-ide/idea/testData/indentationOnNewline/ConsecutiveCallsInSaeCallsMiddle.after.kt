class Some {
    fun some(): Some? = this
}

public fun bar(): String? = Some()?.some()
        <caret>
        ?.some()
        ?.some()
