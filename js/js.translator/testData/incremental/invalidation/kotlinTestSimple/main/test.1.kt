import kotlin.test.Test

class Test1 {
    @Test
    fun foo() {
        sharedValue += "foo"
    }

    @Test
    fun boo() {
        sharedValue += "boo"
    }
}
