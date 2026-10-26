// TARGET_BACKEND: NATIVE
// OUTPUT_DATA_FILE: wideInterfaceTable.out

// The test is about the fast type checks and interface dispatch in optimized binaries linked
// against static caches: the hierarchy the checks are built on is assembled from the caches'
// slices by the final link. Other configurations keep the program correct but do not exercise it.

// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=NO
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST

// MODULE: lib
// FILE: lib.kt
package lib

interface I1 { fun f1(): Int }
interface I2 { fun f2(): Int }
interface I3 { fun f3(): Int }
interface I4 { fun f4(): Int }
interface I5 { fun f5(): Int }
interface I6 { fun f6(): Int }
interface I7 { fun f7(): Int }
interface I8 { fun f8(): Int }
interface I9 { fun f9(): Int }
interface I10 { fun f10(): Int }
interface I11 { fun f11(): Int }
interface I12 { fun f12(): Int }
interface I13 { fun f13(): Int }
interface I14 { fun f14(): Int }
interface I15 { fun f15(): Int }
interface I16 { fun f16(): Int }
interface I17 { fun f17(): Int }
interface I18 { fun f18(): Int }
interface I19 { fun f19(): Int }
interface I20 { fun f20(): Int }
interface I21 { fun f21(): Int }
interface I22 { fun f22(): Int }
interface I23 { fun f23(): Int }
interface I24 { fun f24(): Int }
interface I25 { fun f25(): Int }
interface I26 { fun f26(): Int }
interface I27 { fun f27(): Int }
interface I28 { fun f28(): Int }
interface I29 { fun f29(): Int }
interface I30 { fun f30(): Int }
interface I31 { fun f31(): Int }
interface I32 { fun f32(): Int }
interface I33 { fun f33(): Int }

class Wide : I1, I2, I3, I4, I5, I6, I7, I8, I9, I10, I11, I12, I13, I14, I15, I16, I17, I18, I19, I20, I21, I22, I23, I24, I25, I26, I27, I28, I29, I30, I31, I32, I33 {
    override fun f1() = 1
    override fun f2() = 2
    override fun f3() = 3
    override fun f4() = 4
    override fun f5() = 5
    override fun f6() = 6
    override fun f7() = 7
    override fun f8() = 8
    override fun f9() = 9
    override fun f10() = 10
    override fun f11() = 11
    override fun f12() = 12
    override fun f13() = 13
    override fun f14() = 14
    override fun f15() = 15
    override fun f16() = 16
    override fun f17() = 17
    override fun f18() = 18
    override fun f19() = 19
    override fun f20() = 20
    override fun f21() = 21
    override fun f22() = 22
    override fun f23() = 23
    override fun f24() = 24
    override fun f25() = 25
    override fun f26() = 26
    override fun f27() = 27
    override fun f28() = 28
    override fun f29() = 29
    override fun f30() = 30
    override fun f31() = 31
    override fun f32() = 32
    override fun f33() = 33
}

// MODULE: main(lib)
// FILE: main.kt
import lib.*

fun main() {
    val wide = Wide()
    val sum = (wide as I1).f1() + (wide as I2).f2() + (wide as I3).f3() + (wide as I4).f4() + (wide as I5).f5() + (wide as I6).f6() + (wide as I7).f7() + (wide as I8).f8() + (wide as I9).f9() + (wide as I10).f10() + (wide as I11).f11() + (wide as I12).f12() + (wide as I13).f13() + (wide as I14).f14() + (wide as I15).f15() + (wide as I16).f16() + (wide as I17).f17() + (wide as I18).f18() + (wide as I19).f19() + (wide as I20).f20() + (wide as I21).f21() + (wide as I22).f22() + (wide as I23).f23() + (wide as I24).f24() + (wide as I25).f25() + (wide as I26).f26() + (wide as I27).f27() + (wide as I28).f28() + (wide as I29).f29() + (wide as I30).f30() + (wide as I31).f31() + (wide as I32).f32() + (wide as I33).f33()
    check(sum == 561)
    check(wide is I33)

    println("OK")
}
