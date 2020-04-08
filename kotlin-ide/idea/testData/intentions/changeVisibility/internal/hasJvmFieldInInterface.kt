// IS_APPLICABLE: false
// WITH_RUNTIME

interface KotlinInterface {
    companion object {
        @JvmField
        <caret>val bar = Any()
    }
}