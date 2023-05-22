expect class A1()
expect interface B1
expect annotation class C1()
expect object D1
expect enum class E1 { FOO, BAR, BAZ }

expect class F() {
    inner class G1()
    class H1()
    interface I1
    object J1
    enum class K1 { FOO, BAR, BAZ }
}

expect interface L {
    class M1()
    interface N1
    object O1
    enum class P1 { FOO, BAR, BAZ }
}

expect object R {
    class S1()
    interface T1
    object U1
    enum class V1 { FOO, BAR, BAZ }
}

expect class W() {
    object X {
        interface Y {
            class Z() {
                enum class AA { FOO, BAR, BAZ }
            }
        }
    }
}

expect class BB1() {
    companion object
}

expect class BB2()

expect class CC1() {
    companion object DD1
}

expect class CC2()
expect class CC3()

expect inline class EE1(val value: String)

expect class FF1(property1: String) {
    val property1: String
    val property2: String
    val property3: String
    val property4: String

    fun function1(): String
    fun function2(): String
}

expect class FF2()

expect sealed class GG1
expect sealed class GG2 {
    class HH1() : GG2
    object HH2 : GG2
}

expect class HH5() : GG2
expect object HH6 : GG2

expect enum class II1
expect enum class II2 { FOO }

expect interface JJ {
    val property: String
    fun function(): String
}

expect class KK1(property: String) : JJ {
    override val property: String
    override fun function(): String
}

expect class KK2(wrapped: JJ) : JJ

expect class LL1(value: String) {
    val value: String
}

expect class LL2(value: String) {
    val value: String
}

expect class ExternalClass()