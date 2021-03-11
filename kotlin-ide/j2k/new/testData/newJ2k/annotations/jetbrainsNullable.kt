// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
package test

class Test(str: String?) {
    var myStr: String? = "String2"
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
        Test(test)
    }

    init {
        myStr = str
    }
}