package completion.p23381

class A

fun A.funReference() {}

class InputImpl {
    fun context() {
        <selection>A()::funReference</selection>
    }
}