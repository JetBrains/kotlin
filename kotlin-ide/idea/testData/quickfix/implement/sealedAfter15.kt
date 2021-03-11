// "Implement sealed class" "true"
// WITH_RUNTIME
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces

sealed class <caret>Base {
    abstract fun foo(): Int
}