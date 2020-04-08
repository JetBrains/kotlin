// PROBLEM: none

enum class AccessMode { READ, WRITE, RW }
fun whenExpr(mode: Boolean, access: AccessMode) {
    <caret>if (mode) {
        when (access) {
            AccessMode.READ -> println("read")
            AccessMode.WRITE -> println("write")
        }
    }
    else {
        when (access) {
            AccessMode.READ -> println("noread")
            AccessMode.WRITE -> println("nowrite")
            AccessMode.RW -> println("no both")
        }
    }
}
fun println(s: String) {}