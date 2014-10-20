// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
package test

public class Test(str: String) {
    var myStr = "String2"

    {
        myStr = str
    }

    public fun sout(str: String) {
        // UNNECESSARY_NOT_NULL_ASSERTION heuristic does not work any more, instead we can skip generating !! altogether
        System.out!!.println(str)
    }

    public fun dummy(str: String): String {
        return str
    }

    public fun test() {
        sout("String")
        val test: String = "String2"
        sout(test)
        sout(dummy(test))

        Test(test)
    }
}