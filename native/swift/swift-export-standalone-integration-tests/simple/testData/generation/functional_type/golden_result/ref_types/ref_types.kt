@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("ref_types_internal_functional_type_callee_SwiftU2EOptionalU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Int32__Swift_Optional_data_Bar__Swift_Optional_Swift_String__Swift_Optional_Swift_Set_Swift_AnyHashable____")
internal external fun ref_types_internal_functional_type_callee_SwiftU2EOptionalU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Int32__Swift_Optional_data_Bar__Swift_Optional_Swift_String__Swift_Optional_Swift_Set_Swift_AnyHashable____(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr, _2: kotlin.native.internal.NativePtr, _3: kotlin.native.internal.NativePtr, _4: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("ref_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo__")
internal external fun ref_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("ref_types_internal_functional_type_callee_dataU2EBar__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo_data_Foo__")
internal external fun ref_types_internal_functional_type_callee_dataU2EBar__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo_data_Foo__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr, _2: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Bar__")
internal external fun ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Bar__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ExportedBridge("__root___consume_block_with_opt_reftype__TypesOfArguments__U28Swift_Optional_Swift_Int32__U20Swift_Optional_data_Bar__U20Swift_Optional_Swift_String__U20Swift_Optional_Swift_Set_Swift_AnyHashable__U29202D_U20Swift_Optional_data_Foo___")
public fun __root___consume_block_with_opt_reftype__TypesOfArguments__U28Swift_Optional_Swift_Int32__U20Swift_Optional_data_Bar__U20Swift_Optional_Swift_String__U20Swift_Optional_Swift_Set_Swift_AnyHashable__U29202D_U20Swift_Optional_data_Foo___(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Int?, arg1: Bar?, arg2: kotlin.String?, arg3: kotlin.collections.Set<kotlin.Any>? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else arg0.objcPtr()
            val _arg1 = if (arg1 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg1)
            val _arg2 = if (arg2 == null) kotlin.native.internal.NativePtr.NULL else arg2.objcPtr()
            val _arg3 = if (arg3 == null) kotlin.native.internal.NativePtr.NULL else arg3.objcPtr()
            val _result = ref_types_internal_functional_type_callee_SwiftU2EOptionalU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Int32__Swift_Optional_data_Bar__Swift_Optional_Swift_String__Swift_Optional_Swift_Set_Swift_AnyHashable____(closureBox.objcPtr(), _arg0, _arg1, _arg2, _arg3)
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as Foo
        }
    }
    val _result = run { consume_block_with_opt_reftype(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__")
public fun __root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Boolean {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Foo ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = ref_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo__(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { consume_block_with_reftype_consumer(__block) }
    return run { _result; true }
}

@ExportedBridge("__root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__")
public fun __root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as Foo
        }
    }
    val _result = run { consume_block_with_reftype_factory(__block) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__")
public fun __root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Bar ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Bar__(closureBox.objcPtr(), _arg0)
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as Foo
        }
    }
    val _result = run { consume_block_with_reftype_unzip(__block) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__")
public fun __root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Foo, arg1: Foo ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _arg1 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg1)
            val _result = ref_types_internal_functional_type_callee_dataU2EBar__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo_data_Foo__(closureBox.objcPtr(), _arg0, _arg1)
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as Bar
        }
    }
    val _result = run { consume_block_with_reftype_zip(__block) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
