@file:[JvmName("Test") JvmMultifileClass]
package other

inline fun f(body: () -> Unit) {
    println("i'm other inline function")
    body()
}
