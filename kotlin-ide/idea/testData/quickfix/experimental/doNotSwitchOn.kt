// "Add '-Xopt-in=kotlin.RequiresOptIn' to module light_idea_test_case compiler arguments" "false"
// COMPILER_ARGUMENTS: -version -Xopt-in=Something
// DISABLE-ERRORS
// WITH_RUNTIME
// ACTION: Introduce import alias
// ACTION: Make internal
// ACTION: Make private

@RequiresOptIn<caret>
annotation class MyExperimentalAPI
