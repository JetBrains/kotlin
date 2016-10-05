@NoArg
annotation class NoArg

@NoArg
interface Intf

@NoArg
enum class Colors { RED, WHITE }

@NoArg
object Obj

class MyClass(a: Int) {
    @NoArg
    fun someFun() {}

    @field:NoArg @get:NoArg @set:NoArg
    var abc: String = ""
}