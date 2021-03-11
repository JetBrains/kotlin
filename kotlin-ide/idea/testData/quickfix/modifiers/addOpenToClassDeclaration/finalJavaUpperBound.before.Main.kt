// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ACTION: Convert to secondary constructor
// ACTION: Create test
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Inline type parameter
// ACTION: Introduce import alias
// ACTION: Remove final upper bound
class foo<T : <caret>JavaClass>() {}
