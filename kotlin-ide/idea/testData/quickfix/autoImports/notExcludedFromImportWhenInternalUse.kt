// "Import" "true"
// WITH_RUNTIME
// ACTION: Create local variable 'FunctionReference'
// ACTION: Create object 'FunctionReference'
// ACTION: Create parameter 'FunctionReference'
// ACTION: Create property 'FunctionReference'
// ACTION: Introduce local variable
// ACTION: Rename reference

package kotlin

fun some() {
    FunctionReference<caret>::class
}