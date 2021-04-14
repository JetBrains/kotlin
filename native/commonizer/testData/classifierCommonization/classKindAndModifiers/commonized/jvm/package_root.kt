actual class A1 actual constructor()
interface A2
annotation class A3
object A4
enum class A5 { FOO, BAR, BAZ }

actual interface B1
annotation class B2
object B3
enum class B4 { FOO, BAR, BAZ }

actual annotation class C1 actual constructor()
object C2
enum class C3 { FOO, BAR, BAZ }

actual object D1
enum class D2 { FOO, BAR, BAZ }

actual enum class E1 { FOO, BAR, BAZ }

actual class F actual constructor() {
    actual inner class G1 actual constructor()
    class G2
    interface G3
    object G4
    enum class G5 { FOO, BAR, BAZ }
    companion object G6

    actual class H1 actual constructor()
    interface H2
    object H3
    enum class H4 { FOO, BAR, BAZ }

    actual interface I1
    object I2
    enum class I3 { FOO, BAR, BAZ }

    actual object J1
    enum class J2 { FOO, BAR, BAZ }

    actual enum class K1 { FOO, BAR, BAZ }
}

actual interface L {
    actual class M1 actual constructor()
    interface M2
    object M3
    enum class M4 { FOO, BAR, BAZ }
    companion object M5

    actual interface N1
    object N2
    enum class N3 { FOO, BAR, BAZ }

    actual object O1
    enum class O2 { FOO, BAR, BAZ }

    actual enum class P1 { FOO, BAR, BAZ }
}

actual object R {
    actual class S1 actual constructor()
    interface S2
    object S3
    enum class S4 { FOO, BAR, BAZ }

    actual interface T1
    object T2
    enum class T3 { FOO, BAR, BAZ }

    actual object U1
    enum class U2 { FOO, BAR, BAZ }

    actual enum class V1 { FOO, BAR, BAZ }
}

actual class W actual constructor() {
    actual object X {
        actual interface Y {
            actual class Z actual constructor() {
                actual enum class AA { FOO, BAR, BAZ }
            }
        }
    }
}

actual class BB1 actual constructor() {
    actual companion object
}

actual class BB2 actual constructor()

actual class CC1 actual constructor() {
    actual companion object DD1
}

actual class CC2 actual constructor() {
    companion object CompanionWithAnotherName
}

actual class CC3 actual constructor() {
    companion object
}

actual inline class EE1 actual constructor(actual val value: String)
class EE2(val value: String)

actual class FF1 actual constructor(actual val property1: String) {
    actual val property2 = property1
    actual val property3 get() = property1
    actual val property4 = property1

    actual fun function1() = property1
    actual fun function2() = function1()
}

actual external class FF2 actual constructor()

actual sealed class GG1
actual sealed class GG2 {
    actual class HH1 actual constructor() : GG2()
    actual object HH2 : GG2()
    class HH4 : GG2()
}

actual class HH5 actual constructor() : GG2()
actual object HH6 : GG2()
class HH8 : GG2()

actual enum class II1
actual enum class II2 { FOO, BAZ }

actual interface JJ {
    actual val property: String
    val property2: String
    actual fun function(): String
}

actual class KK1 actual constructor(actual override val property: String) : JJ {
    val property2 = property
    actual override fun function() = property
}

actual class KK2 actual constructor(wrapped: JJ) : JJ by wrapped

actual data class LL1 actual constructor(actual val value: String)
actual class LL2 actual constructor(actual val value: String)
