import kotlin.native.internal.ExportedBridge

@ExportedBridge("foo__TypesOfArguments__uintptr_t_uintptr_t__")
public fun foo(p0: COpaquePointer, p1: COpaquePointer): COpaquePointer {
    val __p0 = dereferenceSpecialRef(p0)
    val __p1 = dereferenceSpecialRef(p1)
    val _result = pkg.foo(__p0, __p1)
    return createSpecialRef(_result)
}
