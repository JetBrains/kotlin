// WITH_STDLIB

annotation class NoArg

@NoArg
sealed class MappedSuperClass

@NoArg
class ConcreteClass(val x: String) : MappedSuperClass()

fun box(): String {
    ConcreteClass::class.java.getConstructor().newInstance()
    return "OK"
}
