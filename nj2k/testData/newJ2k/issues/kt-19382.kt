package test

import java.util.HashMap

class TestPrimitiveFromMap {
    fun foo(map: HashMap<String, Int>): Int {
        return map["zzz"]!!
    }
}