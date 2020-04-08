object O1
object O2

// -----
object O1M {
}
object O2M

// -----

object O3
fun f1() = 1

// -----

object O4
val p1 = 1

// -----

object O5
class C1

// -----

object O5M {
}
class C1M

// -----

// test
object O6
interface T1

// -----

object O6M {
}
interface T1M

// -----

enum class E1 {
    ENTRY;
    object O7
}

// -----

object O8 {}
object O9

// -----

object O8 {
}
object O9

// -----

object O10 {}
fun f2() = 1

// -----

object O11 {}
val p2 = 1

// -----

object O12 {}
class C2

// -----

object O12M {
}
class C2M

// -----

object O13 {}
interface T2

// -----

object O14 {
}
interface T2

// -----

enum class E2 {
    ENTRY;
    object O14 {}
}

// -----

object O15 {
}
object O16

// -----

object O17 {
}
fun f3() = 1

// -----

object O18 {
}
val p3 = 1

// -----

object O19 {
}
class C3

// -----

object O20 {
}
interface T3

// -----

enum class E3 {
    ENTRY;
    object O21 {
    }
}

// -----

sealed class C4 {
    class C5 : C4()
    object O22 : C4()
}

// -----

sealed class C6 {
    class C7 : C6() {
    }
    object O23 : C6()
}
