// WITH_RUNTIME
package foo

class InsertThis {
    val v1 = 1
    fun <caret>f() {
        println(v1)
    }
    fun use() { f() }
}