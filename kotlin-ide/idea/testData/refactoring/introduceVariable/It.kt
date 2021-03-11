fun a(op: (Int) -> Int) {}
fun b() {
    a {it}
    a {
        <selection>it</selection>
    }
}