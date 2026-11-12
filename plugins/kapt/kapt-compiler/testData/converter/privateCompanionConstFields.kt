// WITH_STDLIB

class Holder {
    private companion object {
        const val PUBLIC_CONST = "a"
        private const val PRIVATE_CONST = "b"
        val plain = "c"
        @JvmField val jvmField = "d"
    }
}

class PublicCompanion {
    companion object {
        const val CONST = "e"
    }
}

interface WithPrivateCompanion {
    private companion object {
        const val IN_INTERFACE = "f"
    }
}
