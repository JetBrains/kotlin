// KIND: STANDALONE
// MODULE: SealedTypes
// FILE: main.kt

sealed interface SealedInterfaceA

sealed class SealedClassA : SealedInterfaceA

sealed class SealedClassB : SealedClassA()

class ClassC : SealedClassA() {
    override fun toString(): String = "ClassC"
}

class ClassD : SealedClassB() {
    override fun toString(): String = "ClassD"
}

class ClassE : SealedInterfaceA {
    override fun toString(): String = "ClassE"
}

sealed interface SealedInterfaceB : SealedInterfaceA

interface InterfaceC : SealedInterfaceA

internal class ClassF : SealedInterfaceA {
    override fun toString(): String = "ClassF"
}

internal class ClassG : SealedClassA() {
    override fun toString(): String = "ClassG"
}

@Deprecated("unavailable", level = DeprecationLevel.ERROR)
class ClassH : SealedClassA() {
    override fun toString(): String = "ClassH"
}


fun createClassC_SealedInterfaceA(): SealedInterfaceA = ClassC()

fun createClassC_SealedClassA(): SealedClassA = ClassC()

fun createClassD_SealedClassB(): SealedClassB = ClassD()

fun createClassE_SealedInterfaceA(): SealedInterfaceA = ClassE()

fun createClassF_SealedInterfaceA(): SealedInterfaceA = ClassF()

fun createClassG_SealedClassA(): SealedClassA = ClassG()

@Suppress("DEPRECATION_ERROR")
fun createClassH_SealedClassA(): SealedClassA = ClassH()
