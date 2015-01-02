package demo

class Test {
    fun test(): String {
        val s1 = ""
        val s2 = ""
        val s3 = ""
        if (s1.isEmpty() && s2.isEmpty())
            return "OK"

        if (s1.isEmpty() && s2.isEmpty() && s3.isEmpty())
            return "OOOK"

        return ""
    }
}