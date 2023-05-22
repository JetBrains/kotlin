class A1
class A2
class A3
class A4
class A5

interface B1
interface B2
interface B3
interface B4

annotation class C1
annotation class C2
annotation class C3

object D1
object D2

enum class E1 { FOO, BAR, BAZ }

class F {
    inner class G1
    inner class G2
    inner class G3
    inner class G4
    inner class G5
    inner class G6

    class H1
    class H2
    class H3
    class H4

    interface I1
    interface I2
    interface I3

    object J1
    object J2

    enum class K1 { FOO, BAR, BAZ }
}

interface L {
    class M1
    class M2
    class M3
    class M4
    class M5

    interface N1
    interface N2
    interface N3

    object O1
    object O2

    enum class P1 { FOO, BAR, BAZ }
}

object R {
    class S1
    class S2
    class S3
    class S4

    interface T1
    interface T2
    interface T3

    object U1
    object U2

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

class BB2 {
    companion object
}

class CC1 {
    companion object DD1
}

class CC2 {
    companion object DD2
}

class CC3 {
    companion object DD3
}

inline class EE1(val value: String)
inline class EE2(val value: String)

external class FF1(val property1: String) {
    val property2 = property1
    val property3 get() = property1
    val property4: String

    fun function1() = property1
    fun function2(): String
}

external class FF2()

sealed class GG1
sealed class GG2 {
    class HH1 : GG2()
    object HH2 : GG2()
    class HH3 : GG2()
}

class HH5 : GG2()
object HH6 : GG2()
class HH7 : GG2()

enum class II1
enum class II2 { FOO, BAR }

interface JJ {
    val property: String
    fun function(): String
}

class KK1(override val property: String) : JJ {
    override fun function() = property
}

class KK2(private val wrapped: JJ) : JJ by wrapped

data class LL1(val value: String)
data class LL2(val value: String)

external class ExternalClass