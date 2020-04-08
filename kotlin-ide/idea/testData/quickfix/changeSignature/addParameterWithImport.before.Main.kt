// "Add parameter to function 'foo'" "true"
// ERROR: Too many arguments for public fun foo(): Unit defined in b in file addParameterWithImport.before.Dependency.kt
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
package a

import b.foo

class Bar

fun test() {
    foo(<caret>Bar())
}