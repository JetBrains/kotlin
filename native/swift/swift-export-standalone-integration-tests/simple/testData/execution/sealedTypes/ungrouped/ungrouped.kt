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

// `SealedInterfaceB` has no inheritors, so its case exists but no value can ever reach it
sealed interface SealedInterfaceB : SealedInterfaceA

interface InterfaceC : SealedInterfaceA

internal class ClassF : SealedInterfaceA {
    override fun toString(): String = "ClassF"
}

internal class ClassG : SealedClassA() {
    override fun toString(): String = "ClassG"
}

private class PrivateInterfaceCImpl : InterfaceC {
    override fun toString(): String = "PrivateInterfaceCImpl"
}

fun createClassC_SealedInterfaceA(): SealedInterfaceA = ClassC()

fun createClassC_SealedClassA(): SealedClassA = ClassC()

fun createClassD_SealedClassB(): SealedClassB = ClassD()

fun createClassE_SealedInterfaceA(): SealedInterfaceA = ClassE()

fun createClassF_SealedInterfaceA(): SealedInterfaceA = ClassF()

fun createClassG_SealedClassA(): SealedClassA = ClassG()

fun createPrivateInterfaceCImpl_SealedInterfaceA(): SealedInterfaceA = PrivateInterfaceCImpl()

fun createAnonymousInterfaceC_SealedInterfaceA(): SealedInterfaceA = object : InterfaceC {
    override fun toString(): String = "AnonymousInterfaceC"
}

// FILE: enums_and_generic_interface.kt

// A *direct* enum inheritor gets no case of its own -- enums are dropped from the collected
// inheritors (KT-88682 / KT-88716) -- so it is only reachable through `unknown`. An enum reaching
// the root through a non-sealed leaf interface still matches that interface's case.

enum class EnumClassA : SealedInterfaceA {
    ONE, TWO, THREE
}

enum class EnumClassB : InterfaceC {
    FOUR, FIVE, SIX
}

fun createEnumClassA_SealedInterfaceA(): SealedInterfaceA = EnumClassA.ONE

fun createEnumClassB_SealedInterfaceA(): SealedInterfaceA = EnumClassB.FOUR

// A generic sealed *interface* gets no enum at all (KT-87798), so there is nothing to match from
// Swift; kept here to keep the shape going through export and Swift compilation.
sealed interface QueryResult<T> {
    class Value<T>(val value: T) : QueryResult<T>
    class AsyncValue<T>(val value: T) : QueryResult<T>
}

// FILE: intermediate_interface.kt

sealed interface SealedInterfaceD : SealedInterfaceA

class ClassI : SealedInterfaceD {
    override fun toString(): String = "ClassI"
}

fun createClassI_SealedInterfaceA(): SealedInterfaceA = ClassI()

fun createClassI_SealedInterfaceD(): SealedInterfaceD = ClassI()

// FILE: indirect_subclass.kt

// An `open` non-sealed inheritor and a final subclass of it: dispatch is an `as` cast, so the
// indirect subclass has to arrive as the open inheritor's case, without one of its own.

open class ClassJ : SealedClassA() {
    override fun toString(): String = "ClassJ"
}

class ClassK : ClassJ() {
    override fun toString(): String = "ClassK"
}

fun createClassJ_SealedClassA(): SealedClassA = ClassJ()

fun createClassK_SealedClassA(): SealedClassA = ClassK()
