// "Make '<init>' internal" "false"
// DISABLE-ERRORS
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Introduce import alias
// ACTION: Make 'My' public

internal class My

annotation class Your(val x: <caret>My)