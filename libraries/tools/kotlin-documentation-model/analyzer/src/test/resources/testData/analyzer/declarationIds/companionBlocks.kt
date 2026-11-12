// MODULE: main
// FILE: sample/Companions.kt
package sample

class Outer {
    fun sameName(value: Int) {}

    companion {
        const val CONSTANT = 1
        var mutable: Int = 0
            get() = field
            set(value) {
                field = value
            }
        fun sameName() {}
        fun <T> genericMember(value: T) {}
    }
}

class Named {
    companion {
        fun namedMember() {}
    }
}

class TwoBlocks {
    companion {
        val Zero: TwoBlocks = TwoBlocks()
    }

    fun normalize(): TwoBlocks = this

    companion {
        const val Dimensions: Int = 2
    }
}

interface WithCompanion {
    companion {
        fun interfaceCompanionMember() {}
    }
}

companion fun Outer.topLevelExtension() {}
companion val Outer.topLevelExtensionProperty: String
    get() = ""
companion fun Named.extensionOnNamed() {}
