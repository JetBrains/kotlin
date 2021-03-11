// "Replace with label loop@" "true"

fun foo() {
    @loop<caret> for (i in 1..100) {
        break@loop
    }
}
