package demo

internal class Test {
    fun test(): String {
        val s1 = ""
        val s2 = ""
        val s3 = ""
        if (s1.isEmpty() && s2.isEmpty()) return "OK"
        return if (s1.isEmpty() && s2.isEmpty() && s3.isEmpty()) "OOOK" else ""
    }
}