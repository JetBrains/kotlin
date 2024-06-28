fun testCommon() {
    @OptIn(kotlinx.cinterop.UnsafeNumber::class)
    intPropertyInterop.unsafeProp
}
