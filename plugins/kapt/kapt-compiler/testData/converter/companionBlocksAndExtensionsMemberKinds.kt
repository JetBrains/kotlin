// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB

class A {
    val instanceProperty = 0

    companion {
        var mutable: Int = 1
        const val CONST = "const"
        @JvmField val field = "field"

        fun plain(): String = "plain"
        suspend fun suspended(): Int = 3

        @JvmName("renamed")
        fun named(): String = "named"

        context(c: String)
        fun withContext(x: Int): String = c + x
    }
}

class WithCompanionObject {
    companion {
        fun fromBlock(): String = "block"
    }

    companion object {
        fun fromObject(): String = "object"
    }
}

enum class Level {
    LOW, HIGH;

    companion {
        fun parse(s: String): Level = valueOf(s.uppercase())
    }
}

// Interface companion blocks without properties with accessors.
interface Factory {
    companion {
        fun create(): Factory = object : Factory {}
    }
}

interface Constants {
    companion {
        @JvmField val NAME = "constants"
    }
}

companion fun A.extension(): String = "extension"
companion var A.extensionCounter: Int = 0
