// LANGUAGE: +CompanionBlocks
// ISSUE: KT-90186

interface I {
    companion {
        var prop: Int = 0

        var propWithAccessors: Int = 0
            get() = field
            set(value) { field = value }

        fun foo(): Int = 0
    }
}
