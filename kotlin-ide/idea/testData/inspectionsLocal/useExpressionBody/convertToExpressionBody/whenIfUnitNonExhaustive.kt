// PROBLEM: none

enum class AccessMode { READ, WRITE, RW }
fun whenExpr(mode: Boolean, access: AccessMode) {
    <caret>when (access) {
        AccessMode.READ -> if (mode) println("read") else println("noread")
        AccessMode.WRITE -> if (mode) println("write")
        AccessMode.RW -> if (mode) println("both") else println("no both")
    }
}
fun println(s: String) {}