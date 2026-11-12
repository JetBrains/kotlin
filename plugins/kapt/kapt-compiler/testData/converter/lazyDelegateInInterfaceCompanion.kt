// WITH_STDLIB

interface RegistryTypeMapping {
    companion object {
        private val javaTyped by lazy { java.util.Collections.singletonList("a") }

        val delegated: List<String> by lazy { listOf("a") }
    }
}

class RegistryImpl {
    companion object {
        val delegated: List<String> by lazy { listOf("a") }
    }
}
