import javaApi.Anon5

class A
[Anon5(10)]
(private val a: Int, private val b: Int) {

    deprecated("") // this constructor will not be replaced by default parameter value in primary because of this annotation
    public constructor(a: Int) : this(a, 1) {
    }
}

class B [Anon5(11)]
()

class C [Anon5(12)]
private()