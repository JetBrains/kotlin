class T(val n: Int)

// SIBLING:
fun foo() {
    <selection>if (true) {
        val k = 1
        T().n + k + 1
    }</selection>
}