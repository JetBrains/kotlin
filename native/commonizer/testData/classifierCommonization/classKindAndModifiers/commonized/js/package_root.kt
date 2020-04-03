actual class A1 actual constructor()
class A2
class A3
class A4
class A5

actual interface B1
interface B2
interface B3
interface B4

actual annotation class C1 actual constructor()
annotation class C2
annotation class C3

actual object D1
object D2

actual enum class E1 { FOO, BAR, BAZ }

actual class F actual constructor() {
    actual inner class G1 actual constructor()
    inner class G2
    inner class G3
    inner class G4
    inner class G5
    inner class G6

    actual class H1 actual constructor()
    class H2
    class H3
    class H4

    actual interface I1
    interface I2
    interface I3

    actual object J1
    object J2

    actual enum class K1 { FOO, BAR, BAZ }
}

actual interface L {
    actual class M1 actual constructor()
    class M2
    class M3
    class M4
    class M5

    actual interface N1
    interface N2
    interface N3

    actual object O1
    object O2

    actual enum class P1 { FOO, BAR, BAZ }
}

actual object R {
    actual class S1 actual constructor()
    class S2
    class S3
    class S4

    actual interface T1
    interface T2
    interface T3

    actual object U1
    object U2

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

actual class BB1 actual constructor() { actual companion object }
actual class BB2 actual constructor() { companion object }

actual class CC1 actual constructor() { actual companion object DD1 }
actual class CC2 actual constructor() { companion object DD2 }
actual class CC3 actual constructor() { companion object DD3 }

actual inline class EE1 actual constructor(actual val value: String)
inline class EE2(val value: String)

actual external class FF1 actual constructor(actual val property1: String) {
    actual val property2 = property1
    actual val property3 get() = property1
    actual val property4: String

    actual fun function1() = property1
    actual fun function2(): String
}
actual external class FF2 actual constructor()

actual sealed class GG1
actual sealed class GG2 {
    actual class HH1 actual constructor(): GG2()
    actual object HH2 : GG2()
    class HH3 : GG2()
}

actual class HH5 actual constructor(): GG2()
actual object HH6 : GG2()
class HH7 : GG2()

actual enum class II1
actual enum class II2 { FOO, BAR }

actual interface JJ {
    actual val property: String
    actual fun function(): String
}

actual class KK1 actual constructor(actual override val property: String) : JJ {
    actual override fun function() =  property
}

actual class KK2 actual constructor(private val wrapped: JJ) : JJ by wrapped

actual data class LL1 actual constructor(actual val value: String)
actual data class LL2 actual constructor(actual val value: String)
