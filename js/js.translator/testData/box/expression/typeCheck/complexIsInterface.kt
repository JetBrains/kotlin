// WITH_STDLIB

package foo

interface I1: I61
interface I2: I61, I62
interface I3: I61, I62, I63
interface I4: I61, I62, I63, I64
interface I5: I61, I62, I63, I64, I65
interface I6: I61, I62, I63, I64, I65, I66
interface I7: I61, I62, I63, I64, I65, I66, I67
interface I8: I61, I62, I63, I64, I65, I66, I67, I68
interface I9: I61, I62, I63, I64, I65, I66, I67, I68, I69
interface I10: I61, I62, I63, I64, I65, I66, I67, I68, I69, I70

interface I11: I51, I52, I53, I54, I55, I56, I57, I58, I59, I60
interface I12: I51, I52, I53, I54, I55, I56, I57, I58, I59
interface I13: I51, I52, I53, I54, I55, I56, I57, I58
interface I14: I51, I52, I53, I54, I55, I56, I57
interface I15: I51, I52, I53, I54, I55, I56
interface I16: I51, I52, I53, I54, I55
interface I17: I51, I52, I53, I54
interface I18: I51, I52, I53
interface I19: I51, I52
interface I20: I51

interface I21
interface I22: I21
interface I23: I22
interface I24: I23
interface I25: I24
interface I26: I25
interface I27: I26
interface I28: I27
interface I29: I28
interface I30: I29

interface I31: I40
interface I32: I37
interface I33: I36
interface I34: I35
interface I35
interface I36: I34
interface I37: I33
interface I38: I32
interface I39: I31
interface I40

interface I41: I1
interface I42: I1, I2
interface I43: I1, I2, I3
interface I44: I1, I2, I3, I4
interface I45: I1, I2, I3, I4, I5
interface I46: I1, I2, I3, I4, I5, I6
interface I47: I1, I2, I3, I4, I5, I6, I7
interface I48: I1, I2, I3, I4, I5, I6, I7, I8
interface I49: I1, I2, I3, I4, I5, I6, I7, I8, I9
interface I50: I1, I2, I3, I4, I5, I6, I7, I8, I9, I10

interface I51: I10
interface I52: I20
interface I53: I30
interface I54: I40
interface I55: I50
interface I56: I60
interface I57: I70
interface I58
interface I59
interface I60

interface I61
interface I62
interface I63
interface I64
interface I65
interface I66
interface I67
interface I68
interface I69
interface I70

open class C1: I51
open class C2: C1(), I52
open class C3: C2(), I53
open class C4: C3(), I54
open class C5: C4(), I55
open class C6: C5(), I56
open class C7: C6(), I57
open class C8: C7() // SKIP I58
open class C9: C8() // SKIP I59
class C10: C9(), I60

class C11: I1, I11
class C12: I2, I12
class C13: I3, I13
class C14: I4, I14
class C15: I5, I15
class C16: I6, I16
class C17: I7, I17
class C18: I8, I18
class C19: I9, I19
class C20: I10, I20

open class C21: I31, I32, I33
class C22: C21(), I34, I35, I36
open class C23: I37, I38, I39
class C24: C23(), I40, I50, I60
class C25

fun process(instance: Any, label: String, interfaces: Set<String>) {
    assertEquals("I1" in interfaces, instance is I1, "$label is I1")
    assertEquals("I2" in interfaces, instance is I2, "$label is I2")
    assertEquals("I3" in interfaces, instance is I3, "$label is I3")
    assertEquals("I4" in interfaces, instance is I4, "$label is I4")
    assertEquals("I5" in interfaces, instance is I5, "$label is I5")
    assertEquals("I6" in interfaces, instance is I6, "$label is I6")
    assertEquals("I7" in interfaces, instance is I7, "$label is I7")
    assertEquals("I8" in interfaces, instance is I8, "$label is I8")
    assertEquals("I9" in interfaces, instance is I9, "$label is I9")
    assertEquals("I10" in interfaces, instance is I10, "$label is I10")

    assertEquals("I11" in interfaces, instance is I11, "$label is I11")
    assertEquals("I12" in interfaces, instance is I12, "$label is I12")
    assertEquals("I13" in interfaces, instance is I13, "$label is I13")
    assertEquals("I14" in interfaces, instance is I14, "$label is I14")
    assertEquals("I15" in interfaces, instance is I15, "$label is I15")
    assertEquals("I16" in interfaces, instance is I16, "$label is I16")
    assertEquals("I17" in interfaces, instance is I17, "$label is I17")
    assertEquals("I18" in interfaces, instance is I18, "$label is I18")
    assertEquals("I19" in interfaces, instance is I19, "$label is I19")
    assertEquals("I20" in interfaces, instance is I20, "$label is I20")

    assertEquals("I21" in interfaces, instance is I21, "$label is I21")
    assertEquals("I22" in interfaces, instance is I22, "$label is I22")
    assertEquals("I23" in interfaces, instance is I23, "$label is I23")
    assertEquals("I24" in interfaces, instance is I24, "$label is I24")
    assertEquals("I25" in interfaces, instance is I25, "$label is I25")
    assertEquals("I26" in interfaces, instance is I26, "$label is I26")
    assertEquals("I27" in interfaces, instance is I27, "$label is I27")
    assertEquals("I28" in interfaces, instance is I28, "$label is I28")
    assertEquals("I29" in interfaces, instance is I29, "$label is I29")
    assertEquals("I30" in interfaces, instance is I30, "$label is I30")

    assertEquals("I31" in interfaces, instance is I31, "$label is I31")
    assertEquals("I32" in interfaces, instance is I32, "$label is I32")
    assertEquals("I33" in interfaces, instance is I33, "$label is I33")
    assertEquals("I34" in interfaces, instance is I34, "$label is I34")
    assertEquals("I35" in interfaces, instance is I35, "$label is I35")
    assertEquals("I36" in interfaces, instance is I36, "$label is I36")
    assertEquals("I37" in interfaces, instance is I37, "$label is I37")
    assertEquals("I38" in interfaces, instance is I38, "$label is I38")
    assertEquals("I39" in interfaces, instance is I39, "$label is I39")
    assertEquals("I40" in interfaces, instance is I40, "$label is I40")

    assertEquals("I41" in interfaces, instance is I41, "$label is I41")
    assertEquals("I42" in interfaces, instance is I42, "$label is I42")
    assertEquals("I43" in interfaces, instance is I43, "$label is I43")
    assertEquals("I44" in interfaces, instance is I44, "$label is I44")
    assertEquals("I45" in interfaces, instance is I45, "$label is I45")
    assertEquals("I46" in interfaces, instance is I46, "$label is I46")
    assertEquals("I47" in interfaces, instance is I47, "$label is I47")
    assertEquals("I48" in interfaces, instance is I48, "$label is I48")
    assertEquals("I49" in interfaces, instance is I49, "$label is I49")
    assertEquals("I50" in interfaces, instance is I50, "$label is I50")

    assertEquals("I51" in interfaces, instance is I51, "$label is I51")
    assertEquals("I52" in interfaces, instance is I52, "$label is I52")
    assertEquals("I53" in interfaces, instance is I53, "$label is I53")
    assertEquals("I54" in interfaces, instance is I54, "$label is I54")
    assertEquals("I55" in interfaces, instance is I55, "$label is I55")
    assertEquals("I56" in interfaces, instance is I56, "$label is I56")
    assertEquals("I57" in interfaces, instance is I57, "$label is I57")
    assertEquals("I58" in interfaces, instance is I58, "$label is I58")
    assertEquals("I59" in interfaces, instance is I59, "$label is I59")
    assertEquals("I60" in interfaces, instance is I60, "$label is I60")

    assertEquals("I61" in interfaces, instance is I61, "$label is I61")
    assertEquals("I62" in interfaces, instance is I62, "$label is I62")
    assertEquals("I63" in interfaces, instance is I63, "$label is I63")
    assertEquals("I64" in interfaces, instance is I64, "$label is I64")
    assertEquals("I65" in interfaces, instance is I65, "$label is I65")
    assertEquals("I66" in interfaces, instance is I66, "$label is I66")
    assertEquals("I67" in interfaces, instance is I67, "$label is I67")
    assertEquals("I68" in interfaces, instance is I68, "$label is I68")
    assertEquals("I69" in interfaces, instance is I69, "$label is I69")
    assertEquals("I70" in interfaces, instance is I70, "$label is I70")
}

fun box(): String {
    process(C1(), "C1", setOf("I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C2(), "C2", setOf("I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C3(), "C3", setOf("I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C4(), "C4", setOf("I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C5(), "C5", setOf("I55", "I50", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C6(), "C6", setOf("I56", "I60", "I55", "I50", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C7(), "C7", setOf("I57", "I56", "I60", "I55", "I50", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C8(), "C8", setOf("I57", "I56", "I60", "I55", "I50", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C9(), "C9", setOf("I57", "I56", "I60", "I55", "I50", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C10(), "C10", setOf("I57", "I56", "I60", "I55", "I50", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I54", "I40", "I53", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I52", "I20", "I51", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))

    process(C11(), "C11", setOf("I1", "I61", "I11", "I51", "I52", "I53", "I54", "I55", "I56", "I57", "I58", "I59", "I60", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70", "I10", "I20", "I30", "I40", "I50", "I60", "I70", "I21", "I22", "I23", "I24", "I25", "I26", "I27", "I28", "I29", "I30", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10"))
    process(C12(), "C12", setOf("I2", "I61", "I62", "I12", "I51", "I52", "I53", "I54", "I55", "I56", "I57", "I58", "I59", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70", "I10", "I20", "I30", "I40", "I50", "I70", "I21", "I22", "I23", "I24", "I25", "I26", "I27", "I28", "I29", "I30", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I60"))
    process(C13(), "C13", setOf("I3", "I13", "I61", "I62", "I63", "I51", "I52", "I53", "I54", "I55", "I56", "I57", "I58", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70", "I10", "I20", "I30", "I40", "I50", "I70", "I21", "I22", "I23", "I24", "I25", "I26", "I27", "I28", "I29", "I30", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I60"))
    process(C14(), "C14", setOf("I4", "I14", "I61", "I62", "I63", "I64", "I51", "I52", "I53", "I54", "I55", "I56", "I57", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70", "I10", "I20", "I30", "I40", "I50", "I70", "I21", "I22", "I23", "I24", "I25", "I26", "I27", "I28", "I29", "I30", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I60"))
    process(C15(), "C15", setOf("I5", "I61", "I62", "I63", "I64", "I65", "I15", "I51", "I52", "I53", "I54", "I55", "I56", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70", "I10", "I20", "I30", "I40", "I50", "I21", "I22", "I23", "I24", "I25", "I26", "I27", "I28", "I29", "I30", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I60"))
    process(C16(), "C16", setOf("I6", "I61", "I62", "I63", "I64", "I65", "I66", "I16", "I51", "I52", "I53", "I54", "I55", "I10", "I20", "I30", "I40", "I50", "I67", "I68", "I69", "I70", "I51", "I29", "I21", "I22", "I23", "I24", "I25", "I26", "I27", "I28", "I29", "I30", "I40", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I67", "I68", "I69", "I70"))
    process(C17(), "C17", setOf("I7", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I17", "I51", "I52", "I53", "I54", "I10", "I68", "I69", "I70", "I20", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21", "I40"))
    process(C18(), "C18", setOf("I8", "I18", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I51", "I52", "I53", "I10", "I69", "I70", "I20", "I30", "I29", "I28", "I27", "I26", "I25", "I24", "I23", "I22", "I21"))
    process(C19(), "C19", setOf("I9", "I19", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I51", "I52", "I10", "I70", "I20"))
    process(C20(), "C20", setOf("I10", "I20", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70", "I51"))

    process(C21(), "C21", setOf("I31", "I32", "I33", "I40", "I37", "I33", "I36", "I34", "I35"))
    process(C22(), "C22", setOf("I31", "I32", "I33", "I40", "I37", "I33", "I36", "I34", "I35"))
    process(C23(), "C23", setOf("I37", "I38", "I39", "I33", "I36", "I34", "I35", "I32", "I31", "I40"))
    process(C24(), "C24", setOf("I37", "I38", "I39", "I33", "I36", "I34", "I35", "I32", "I31", "I40", "I50", "I60", "I1", "I2", "I3", "I4", "I5", "I6", "I7", "I8", "I9", "I10", "I61", "I62", "I63", "I64", "I65", "I66", "I67", "I68", "I69", "I70"))
    process(C25(), "C25", emptySet())

    return "OK"
}
