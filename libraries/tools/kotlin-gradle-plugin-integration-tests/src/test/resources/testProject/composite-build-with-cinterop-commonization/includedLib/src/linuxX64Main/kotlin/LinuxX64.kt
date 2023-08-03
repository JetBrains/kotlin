package includedLib

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun linuxX64(
    a: cinterop.a.StructA
) {
    cinterop.a.a()
    cinterop.b.b()
}