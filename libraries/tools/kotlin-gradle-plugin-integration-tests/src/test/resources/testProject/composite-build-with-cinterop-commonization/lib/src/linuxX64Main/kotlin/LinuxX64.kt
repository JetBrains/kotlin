package lib

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun linuxX64(
    a: cinterop.lib.a.StructA
) {
    cinterop.lib.a.a()
    cinterop.lib.b.b()
}