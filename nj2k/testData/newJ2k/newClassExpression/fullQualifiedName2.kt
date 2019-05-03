// ERROR: One type argument expected for interface List<out E>
package test

internal class User {
    fun main() {
        val list: List<*> = ArrayList<Any?>()
    }
}
