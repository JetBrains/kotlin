package includedLib

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun linuxArm64(
    a: cinterop.a.StructA
) {
    cinterop.a.a()
    cinterop.c.c()
}