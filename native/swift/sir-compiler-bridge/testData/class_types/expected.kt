import kotlin.native.internal.ExportedBridge

@ExportedBridge("foo__TypesOfArguments__uintptr_t_uintptr_t__")
public fun foo(p0: kotlin.native.internal.NativePtr, p1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __p0 = kotlin.native.internal.ref.dereferenceExternalRCRef(p0) as MyClass
    val __p1 = kotlin.native.internal.ref.dereferenceExternalRCRef(p1) as MyClass
    val _result = pkg.foo(__p0, __p1)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
