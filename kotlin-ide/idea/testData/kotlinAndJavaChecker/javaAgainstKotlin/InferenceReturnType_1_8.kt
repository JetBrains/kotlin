@file: JvmName("Util")

package a

class A<T>()

class ValueManager() {
    companion object {
        fun <Z> reject() = null as A<Z>
    }
}