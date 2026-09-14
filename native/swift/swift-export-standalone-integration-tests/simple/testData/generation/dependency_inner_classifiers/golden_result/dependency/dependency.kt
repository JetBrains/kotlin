@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(datetime.LocalDate::class, "22ExportedKotlinPackages8datetimeO10dependencyE9LocalDateC")
@file:kotlin.native.internal.objc.BindClassToObjCName(datetime.LocalDate.Companion::class, "22ExportedKotlinPackages8datetimeO10dependencyE9LocalDateC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(datetime.DateTimeFormat::class, "_ExportedKotlinPackages_datetime_DateTimeFormat")
@file:kotlin.native.internal.objc.BindClassToObjCName(datetime.DateTimeFormatBuilder::class, "_ExportedKotlinPackages_datetime_DateTimeFormatBuilder")
@file:kotlin.native.internal.objc.BindClassToObjCName(datetime.DateTimeFormatBuilder.WithDate::class, "_dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("dependency_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate__")
internal external fun dependency_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate__(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ExportedBridge("datetime_LocalDate_Companion_Format__TypesOfArguments__U28anyU20dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDateU29202D_U20Swift_Void__")
public fun datetime_LocalDate_Companion_Format__TypesOfArguments__U28anyU20dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDateU29202D_U20Swift_Void__(self: kotlin.native.internal.NativePtr, block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as datetime.LocalDate.Companion
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: datetime.DateTimeFormatBuilder.WithDate ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = dependency_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate__(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    val _result = run { __self.Format(__block) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("datetime_LocalDate_Companion_get")
public fun datetime_LocalDate_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = run { datetime.LocalDate.Companion }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("datetime_LocalDate_init_allocate")
public fun datetime_LocalDate_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<datetime.LocalDate>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("datetime_LocalDate_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun datetime_LocalDate_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, datetime.LocalDate()) }
    return run { _result; true }
}
