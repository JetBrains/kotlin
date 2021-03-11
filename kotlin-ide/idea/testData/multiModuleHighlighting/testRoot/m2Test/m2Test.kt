package shared.test

import shared.*

private fun privateInM2Test() {
}
internal fun internalInM2Test() {
}
public fun publicInM2Test() {
}

fun access() {
    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1': it is private in file">privateInM1</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM1': it is internal in 'shared'">internalInM1</error>()
    publicInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1Test': it is private in file">privateInM1Test</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM1Test': it is internal in 'shared.test'">internalInM1Test</error>()
    publicInM1Test()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM2': it is private in file">privateInM2</error>()
    internalInM2()
    publicInM2()

    privateInM2Test()
    internalInM2Test()
    publicInM2Test()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM3">privateInM3</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM3">internalInM3</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM3">publicInM3</error>()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM3Test">privateInM3Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM3Test">internalInM3Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM3Test">publicInM3Test</error>()
}
