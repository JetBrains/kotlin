// IS_APPLICABLE: false
// WITH_RUNTIME

interface KotlinInterface {
    companion object {
        @JvmField
        val <caret>bar = Any()
    }
}