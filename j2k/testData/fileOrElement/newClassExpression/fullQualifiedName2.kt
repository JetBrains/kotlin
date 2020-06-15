// ERROR: Type inference failed: Not enough information to infer parameter E in constructor ArrayList<E : Any!>() Please specify it explicitly.
package test

internal class User {
    fun main() {
        val list = java.util.ArrayList()
    }
}