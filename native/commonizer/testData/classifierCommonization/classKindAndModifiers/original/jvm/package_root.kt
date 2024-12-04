class A1
interface A2
annotation class A3
object A4
enum class A5 { FOO, BAR, BAZ }

interface B1
annotation class B2
object B3
enum class B4 { FOO, BAR, BAZ }

annotation class C1
object C2
enum class C3 { FOO, BAR, BAZ }

object D1
enum class D2 { FOO, BAR, BAZ }

enum class E1 { FOO, BAR, BAZ }

class F {
    inner class G1
    class G2
    interface G3
    object G4
    enum class G5 { FOO, BAR, BAZ }
    companion object G6

    class H1
    interface H2
    object H3
    enum class H4 { FOO, BAR, BAZ }

    interface I1
    object I2
    enum class I3 { FOO, BAR, BAZ }

    object J1
    enum class J2 { FOO, BAR, BAZ }

    enum class K1 { FOO, BAR, BAZ }
}

interface L {
    class M1
    interface M2
    object M3
    enum class M4 { FOO, BAR, BAZ }
    companion object M5

    interface N1
    object N2
    enum class N3 { FOO, BAR, BAZ }

    object O1
    enum class O2 { FOO, BAR, BAZ }

    enum class P1 { FOO, BAR, BAZ }
}

object R {
    class S1
    interface S2
    object S3
    enum class S4 { FOO, BAR, BAZ }

    interface T1
    object T2
    enum class T3 { FOO, BAR, BAZ }

    object U1
    enum class U2 { FOO, BAR, BAZ }

    enum class V1 { FOO, BAR, BAZ }
}

class W {
    object X {
        interface Y {
            class Z {
                enum class AA { FOO, BAR, BAZ }
            }
        }
    }
}

class BB1 {
    companion object
}

class BB2

class CC1 {
    companion object DD1
}

class CC2 {
    companion object CompanionWithAnotherName
}

class CC3 {
    companion object
}

inline class EE1(val value: String)
class EE2(val value: String)

class FF1(val property1: String) {
    val property2 = property1
    val property3 get() = property1
    val property4 = property1

    fun function1() = property1
    fun function2() = function1()
}

external class FF2()

sealed class GG1
sealed class GG2 {
    class HH1 : GG2()
    object HH2 : GG2()
    class HH4 : GG2()
}

class HH5 : GG2()
object HH6 : GG2()
class HH8 : GG2()

enum class II1
enum class II2 { FOO, BAZ }

interface JJ {
    val property: String
    val property2: String
    fun function(): String
}

class KK1(override val property: String) : JJ {
    val property2 = property
    override fun function() = property
}

class KK2(wrapped: JJ) : JJ by wrapped

data class LL1(val value: String)
class LL2(val value: String)

external class ExternalClass