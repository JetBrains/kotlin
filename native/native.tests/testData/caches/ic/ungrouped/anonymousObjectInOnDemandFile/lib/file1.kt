package test

interface Answer {
    val value: Int get() = 42
}

class C {
    // The return type leaks the anonymous object, so its class is deserialized together with `C`
    // even when this file is only loaded on demand while building the per-file cache of `file2.kt`.
    private fun answer() = object : Answer {
        fun twice(): Int = value * 2
    }

    fun compute(): Int = answer().twice()
}
