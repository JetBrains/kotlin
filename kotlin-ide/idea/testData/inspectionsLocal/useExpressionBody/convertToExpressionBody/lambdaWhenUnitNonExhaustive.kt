// IS_APPLICABLE: true
enum class AccessMode { READ, WRITE, RW }
fun <T> run(f: () -> T) = f()
fun whenExpr(access: AccessMode) {
    <caret>run {
        println("run")
        when (access) {
            AccessMode.READ -> println("read")
            AccessMode.WRITE -> println("write")
        }
    }
}
fun println(s: String) {}