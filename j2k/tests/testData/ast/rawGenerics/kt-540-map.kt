package demo

import java.util.HashMap

open class Test() {
    open fun main() {
        var commonMap: HashMap<String?, Int?>? = HashMap<String?, Int?>()
        var rawMap: HashMap<Any?, Any?>? = HashMap<String?, Int?>()
        var superRawMap: HashMap<Any?, Any?>? = HashMap()
    }
}