// WITH_RUNTIME
const val ONE = 1

fun foo() {
    2..1
    2.rangeTo(1)
    2..1L
    10L..-10L
    5..ONE
    10.toShort()..1.toShort()

    //valid
    1..1
    1..10L
    1..2
    1.rangeTo(2)
}