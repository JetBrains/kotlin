package test.regressions.kt1149

import java.util.ArrayList
import std.util.*
import junit.framework.*

public trait SomeTrait {
    fun foo()
}

class Kt1149Test() : TestCase() {
    fun testMe() {
        val list = ArrayList<SomeTrait>()
        var res = ArrayList<String>()
        list.add(object : SomeTrait {
            override fun foo() {
                res.add("anonymous.foo()")
            }
        })
        list.foreach{ it.foo() }
        Assert.assertEquals("anonymous.foo()", res[0])
    }
}
