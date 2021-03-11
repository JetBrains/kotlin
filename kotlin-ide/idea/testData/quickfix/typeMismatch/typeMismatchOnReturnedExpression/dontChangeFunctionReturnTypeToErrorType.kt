// "Change 'foo' function return type to '(x: [ERROR : NoSuchType]) -> Int'" "false"
// ACTION: Convert to multi-line lambda
// ACTION: Create annotation 'NoSuchType'
// ACTION: Create class 'NoSuchType'
// ACTION: Create enum 'NoSuchType'
// ACTION: Create interface 'NoSuchType'
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Remove explicit lambda parameter types (may break code)
// ACTION: Create type parameter 'NoSuchType' in function 'foo'
// ERROR: Type mismatch: inferred type is ([ERROR : NoSuchType]) -> Int but Int was expected
// ERROR: Unresolved reference: NoSuchType

fun foo(): Int {
    return { x: NoSuchType<caret> -> 42 }
}