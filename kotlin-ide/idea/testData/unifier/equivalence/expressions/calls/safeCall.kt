fun Any.s(): String = toString()

fun foo() {
    Any().s()
    <selection>Any()?.s()</selection>
}