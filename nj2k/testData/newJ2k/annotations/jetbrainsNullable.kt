// !specifyLocalVariableTypeByDefault: true
package test

class Test(internal var myStr: String?) {
    fun sout(str: String?) {
        println(str)
    }

    fun dummy(str: String?): String? {
        return str
    }

    fun test() {
        sout("String")
        val test: String = "String2"
        sout(test)
        sout(dummy(test))

        Test(test)
    }
}