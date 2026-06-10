// COMPILER_PLUGIN: kotlin-noarg-compiler-plugin-2.4.20.jar annotation=NoArg
// FULL_JDK

annotation class NoArg

@NoArg
class Entity(val id: Int, val name: String)

class Foo {
    fun load() {
        val entity = this::class.java.classLoader.loadClass("Entity").getDeclaredConstructor().newInstance()
    }
}

fun box(): String {
    Foo().load()
    return "OK"
}
