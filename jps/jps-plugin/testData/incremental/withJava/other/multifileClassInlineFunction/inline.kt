@file:[JvmName("Test") JvmMultifileClass]
package test

inline fun f(body: () -> Unit) {
    println("i'm inline function")
    body()
}
