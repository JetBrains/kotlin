import foo.GeneratedEnum
import java.util.Arrays
import org.jetbrains.kotlin.fir.plugin.GenerateEnumConstant

@GenerateEnumConstant
class A

@GenerateEnumConstant
class B

@GenerateEnumConstant
class C

fun testEnumEntryVariables() {
    val entryA: GeneratedEnum = GeneratedEnum.A
    val entryB: GeneratedEnum = GeneratedEnum.B
    val entryC: GeneratedEnum = GeneratedEnum.C

    if (entryA.name != "A" || entryA.ordinal != 0) throw IllegalArgumentException()
    if (entryB.name != "B" || entryB.ordinal != 1) throw IllegalArgumentException()
    if (entryC.name != "C" || entryC.ordinal != 2) throw IllegalArgumentException()
}

fun testEnumValues() {
    val values = GeneratedEnum.values()

    if (values.size != 3) throw IllegalArgumentException()
    if (values[0] != GeneratedEnum.A) throw IllegalArgumentException()
    if (values[1] != GeneratedEnum.B) throw IllegalArgumentException()
    if (values[2] != GeneratedEnum.C) throw IllegalArgumentException()
}

fun testEnumValueOf() {
    if (GeneratedEnum.A != GeneratedEnum.valueOf("A")) throw IllegalArgumentException()
    if (GeneratedEnum.B != GeneratedEnum.valueOf("B")) throw IllegalArgumentException()
    if (GeneratedEnum.C != GeneratedEnum.valueOf("C")) throw IllegalArgumentException()
}

fun testEnumEntries() {
    val entries = GeneratedEnum.entries

    if (entries.size != 3) throw IllegalArgumentException()
    if (entries[0] != GeneratedEnum.A) throw IllegalArgumentException()
    if (entries[1] != GeneratedEnum.B) throw IllegalArgumentException()
    if (entries[2] != GeneratedEnum.C) throw IllegalArgumentException()
}

fun box(): String {
    testEnumEntryVariables()
    testEnumValues()
    testEnumValueOf()
    testEnumEntries()

    return "OK"
}
