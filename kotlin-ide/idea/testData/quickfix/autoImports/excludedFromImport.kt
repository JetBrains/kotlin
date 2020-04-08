// "Import" "false"
// WITH_RUNTIME
// ACTION: Create local variable 'FunctionReference'
// ACTION: Create object 'FunctionReference'
// ACTION: Create parameter 'FunctionReference'
// ACTION: Create property 'FunctionReference'
// ACTION: Create annotation 'FunctionReference'
// ACTION: Create class 'FunctionReference'
// ACTION: Create enum 'FunctionReference'
// ACTION: Create interface 'FunctionReference'
// ACTION: Introduce local variable
// ACTION: Rename reference
// ERROR: Unresolved reference: FunctionReference

fun some() {
    FunctionReference<caret>::class
}