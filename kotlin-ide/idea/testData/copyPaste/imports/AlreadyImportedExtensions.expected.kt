// ERROR: Unresolved reference: s
// ERROR: Unresolved reference: s
// ERROR: Unresolved reference: s
// ERROR: Unresolved reference: s
fun foo() {
    s.extensionFun()
    for (i in s.indices) {
        val pair = s to null
        val s1 = s + "1"
    }

}
