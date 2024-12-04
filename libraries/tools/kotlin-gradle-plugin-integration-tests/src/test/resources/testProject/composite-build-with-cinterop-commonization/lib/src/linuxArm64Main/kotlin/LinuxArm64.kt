package lib

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun linuxArm64(
    a: cinterop.lib.a.StructA
) {
    cinterop.lib.a.a()
    cinterop.lib.c.c()
}