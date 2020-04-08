// WITH_RUNTIME

class Test {
    fun doAThing(param1: String): String {
        return param1
    }

    fun doAThingIfPresent(param1: String?): String {
        return <caret>if (param1 != null) {
            doAThing(param1)
        } else {
            ""
        }
    }
}