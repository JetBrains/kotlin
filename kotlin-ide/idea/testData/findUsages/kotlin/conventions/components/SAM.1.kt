import pack.A

fun foo(javaClass: JavaClass) {
    javaClass.takeSAM { val (x, y) = it }
}