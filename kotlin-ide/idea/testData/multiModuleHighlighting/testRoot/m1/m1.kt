package shared

import shared.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: test">test</error>.*

private fun privateInM1() {
}
internal fun internalInM1() {
}
public fun publicInM1() {
}

fun access() {
    privateInM1()
    internalInM1()
    publicInM1()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM1Test">privateInM1Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM1Test">internalInM1Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM1Test">publicInM1Test</error>()

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