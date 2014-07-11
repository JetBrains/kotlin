import javaApi.Anon5


deprecated("") // this constructor will not be replaced by default parameter value in primary because of this annotation
fun A(a: Int): A {
    return A(a, 1)
}

class A
[Anon5(10)]
(private val a: Int, private val b: Int)

class B [Anon5(11)]
()

class C [Anon5(12)]
private()