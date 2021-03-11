//ALLOW_AST_ACCESS
package test

annotation class Anno

interface Trait {
    fun foo(@[Anno] x: String) = 42
}
