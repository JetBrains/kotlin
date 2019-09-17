// ERROR: Unresolved reference: Test
// !specifyLocalVariableTypeByDefault: true
package test

class Test(str: String?) {
    internal var myStr: String? = "String2"
    fun sout(str: String?) {
        println(str)
    }

    fun dummy(str: String?): String? {
        return str
    }

    fun test() {
        sout("String")
        val test = "String2"
        sout(test)
        sout(dummy(test))
        test.Test(test)
    }

    init {
        myStr = str
    }
}