// ERROR: This annotation is not applicable to target 'local variable'
// ERROR: This annotation is not applicable to target 'value parameter'
// ERROR: This annotation is not applicable to target 'value parameter'
import javaApi.*

@Anon1(value = ["a"], stringArray = ["b"], intArray = [1, 2], string = "x")
@Anon2(value = "a", intValue = 1, charValue = 'a')
@Anon3(e = E.A, stringArray = [], value = ["a", "b"])
@Anon4("x", "y")
@Anon5(1)
@Anon6("x", "y")
@Anon7(String::class, StringBuilder::class)
@Anon8(classes = [String::class, StringBuilder::class])
internal class C {
    @Anon5(1)
    @Deprecated("")
    private val field1 = 0

    @Anon5(1)
    private val field2 = 0

    @Anon5(1)
    var field3 = 0

    @Anon5(1)
    var field4 = 0

    @Anon6
    fun foo(@Deprecated("") p1: Int, @Deprecated("") @Anon5(2) p2: Char) {
        @Deprecated("") @Anon5(3) val c = 'a'
    }

    @Anon5(1)
    fun bar() {
    }
}
