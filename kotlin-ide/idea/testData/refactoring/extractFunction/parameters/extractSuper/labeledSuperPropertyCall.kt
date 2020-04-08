public open class Z {
    val z: Int = 0
}

// SIBLING:
public class A(): Z() {
    var a: Int = 1

    fun foo(): Int {
        <selection>return a + super<Z>@A.z</selection>
    }
}
