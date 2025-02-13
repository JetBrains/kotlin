@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("foo__TypesOfArguments__MyClass_MyClass__")
public fun foo__TypesOfArguments__MyClass_MyClass__(p0: kotlin.native.internal.NativePtr, p1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __p0 = kotlin.native.internal.ref.dereferenceExternalRCRef(p0) as MyClass
    val __p1 = kotlin.native.internal.ref.dereferenceExternalRCRef(p1) as MyClass
    val _result = pkg.foo(__p0, __p1)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
