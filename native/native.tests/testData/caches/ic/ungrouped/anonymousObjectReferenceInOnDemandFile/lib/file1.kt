package test

interface Answer {
    val value: Int get() = 42
    fun half(): Int = value / 2
}

class C {
    // The return type leaks the anonymous object, so its class is deserialized together with `C`
    // even when this file is only loaded on demand while building the per-file cache of `file2.kt`.
    // References, unlike calls, to fake overrides of the anonymous class.
    private fun answer() = object : Answer {
        fun viaReferences(): Int = this::value.get() + this::half.invoke()
    }

    fun compute(): Int = answer().viaReferences()
}
