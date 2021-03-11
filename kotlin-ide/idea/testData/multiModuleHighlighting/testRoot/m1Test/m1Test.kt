package shared.test

import shared.*

private fun privateInM1Test() {
}
internal fun internalInM1Test() {
}
public fun publicInM1Test() {
}

fun access() {
    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1': it is private in file">privateInM1</error>()
    internalInM1()
    publicInM1()

    privateInM1Test()
    internalInM1Test()
    publicInM1Test()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM2">privateInM2</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM2">internalInM2</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM2">publicInM2</error>()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM2Test">privateInM2Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM2Test">internalInM2Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM2Test">publicInM2Test</error>()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM3">privateInM3</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM3">internalInM3</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM3">publicInM3</error>()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM3Test">privateInM3Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM3Test">internalInM3Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM3Test">publicInM3Test</error>()
}
