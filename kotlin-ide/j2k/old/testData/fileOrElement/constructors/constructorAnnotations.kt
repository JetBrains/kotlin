import javaApi.Anon5

internal class A @Anon5(10)
constructor(private val a: Int, private val b: Int) {

    @Deprecated("") // this constructor will not be replaced by default parameter value in primary because of this annotation
    constructor(a: Int) : this(a, 1) {
    }
}

internal class B @Anon5(11)
constructor()

internal class C @Anon5(12)
private constructor()