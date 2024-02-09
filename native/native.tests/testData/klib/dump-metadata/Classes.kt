// FIR_IDENTICAL
class A {
    fun aFun() {}
    val aVal = 0
    var aVar = ""

    inner class B {
        fun bFun() {}
        val bVal = 0
        var bVar = ""

        inner class C {
            fun cFun() {}
            val cVal = 0
            var cVar = ""
        }

        private inner class D {
            fun dFun() {}
            val dVal = 0
            var dVar = ""
        }
    }

    class E {
        fun eFun() {}
        val eVal = 0
        var eVar = ""
    }

}

data class F(val fVal: Int, var fVar: String) {
    fun fFun() {}
}

interface Interface {
    fun iFun()
    val iVal: Int
    var iVar: String
}

open class OpenImpl: Interface {
    override fun iFun() {}
    override val iVal = 0
    override var iVar = ""
}

class FinalImpl: OpenImpl() {
    override fun iFun() {}
    override val iVal = 0
    override var iVar = ""
}
