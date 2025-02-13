@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedChildT::class, "4main16deprecatedChildTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT::class, "4main11deprecatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT.deprecationInheritedT::class, "4main11deprecatedTC21deprecationInheritedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT.deprecationRestatedT::class, "4main11deprecatedTC20deprecationRestatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalChildT::class, "4main12normalChildTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT::class, "4main7normalTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT.deprecatedT::class, "4main7normalTC11deprecatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT.normalT::class, "4main7normalTC7normalTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(renamedT::class, "4main8renamedTC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___MESSAGE_get")
public fun __root___MESSAGE_get(): kotlin.native.internal.NativePtr {
    val _result = MESSAGE
    return _result.objcPtr()
}

@ExportedBridge("__root___constMessage")
public fun __root___constMessage(): kotlin.native.internal.NativePtr {
    val _result = constMessage()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedChildT_init_allocate")
public fun __root___deprecatedChildT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedChildT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedChildT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___deprecatedChildT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, deprecatedChildT())
}

@ExportedBridge("__root___deprecatedF")
public fun __root___deprecatedF(): Unit {
    deprecatedF()
}

@ExportedBridge("__root___deprecatedImplicitlyF")
public fun __root___deprecatedImplicitlyF(): Unit {
    deprecatedImplicitlyF()
}

@ExportedBridge("__root___deprecatedT_init_allocate")
public fun __root___deprecatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___deprecatedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, deprecatedT())
}

@ExportedBridge("__root___deprecationInheritedImplicitlyV_get")
public fun __root___deprecationInheritedImplicitlyV_get(): Unit {
    deprecationInheritedImplicitlyV
}

@ExportedBridge("__root___deprecationInheritedV_get")
public fun __root___deprecationInheritedV_get(): Unit {
    deprecationInheritedV
}

@ExportedBridge("__root___formattedMessage")
public fun __root___formattedMessage(): kotlin.native.internal.NativePtr {
    val _result = formattedMessage()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___multilineFormattedMessage")
public fun __root___multilineFormattedMessage(): kotlin.native.internal.NativePtr {
    val _result = multilineFormattedMessage()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___multilineMessage")
public fun __root___multilineMessage(): kotlin.native.internal.NativePtr {
    val _result = multilineMessage()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalChildT_init_allocate")
public fun __root___normalChildT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalChildT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalChildT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___normalChildT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, normalChildT())
}

@ExportedBridge("__root___normalT_init_allocate")
public fun __root___normalT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___normalT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, normalT())
}

@ExportedBridge("__root___obsoletedF")
public fun __root___obsoletedF(): Unit {
    obsoletedF()
}

@ExportedBridge("__root___obsoletedV_get")
public fun __root___obsoletedV_get(): Unit {
    obsoletedV
}

@ExportedBridge("__root___renamed__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamed__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = renamed(__x, __y)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedF")
public fun __root___renamedF(): Unit {
    renamedF()
}

@ExportedBridge("__root___renamedQualified__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamedQualified__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = renamedQualified(__x, __y)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedQualifiedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamedQualifiedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = renamedQualifiedWithArguments(__x, __y)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedT_init_allocate")
public fun __root___renamedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<renamedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___renamedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, renamedT())
}

@ExportedBridge("__root___renamedV_get")
public fun __root___renamedV_get(): Unit {
    renamedV
}

@ExportedBridge("__root___renamedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = renamedWithArguments(__x, __y)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___unrenamed")
public fun __root___unrenamed(): kotlin.native.internal.NativePtr {
    val _result = unrenamed()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedChildT_deprecationFurtherReinforcedF")
public fun deprecatedChildT_deprecationFurtherReinforcedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    __self.deprecationFurtherReinforcedF()
}

@ExportedBridge("deprecatedChildT_deprecationFurtherReinforcedV_get")
public fun deprecatedChildT_deprecationFurtherReinforcedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    __self.deprecationFurtherReinforcedV
}

@ExportedBridge("deprecatedChildT_deprecationReinforcedF")
public fun deprecatedChildT_deprecationReinforcedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    __self.deprecationReinforcedF()
}

@ExportedBridge("deprecatedChildT_deprecationReinforcedV_get")
public fun deprecatedChildT_deprecationReinforcedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    __self.deprecationReinforcedV
}

@ExportedBridge("deprecatedChildT_deprecationRestatedF")
public fun deprecatedChildT_deprecationRestatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    __self.deprecationRestatedF()
}

@ExportedBridge("deprecatedChildT_deprecationRestatedV_get")
public fun deprecatedChildT_deprecationRestatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    __self.deprecationRestatedV
}

@ExportedBridge("deprecatedT_deprecationInheritedF")
public fun deprecatedT_deprecationInheritedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationInheritedF()
}

@ExportedBridge("deprecatedT_deprecationInheritedT_init_allocate")
public fun deprecatedT_deprecationInheritedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationInheritedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationInheritedT())
}

@ExportedBridge("deprecatedT_deprecationInheritedV_get")
public fun deprecatedT_deprecationInheritedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationInheritedV
}

@ExportedBridge("deprecatedT_deprecationReinforcedF")
public fun deprecatedT_deprecationReinforcedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationReinforcedF()
}

@ExportedBridge("deprecatedT_deprecationReinforcedV_get")
public fun deprecatedT_deprecationReinforcedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationReinforcedV
}

@ExportedBridge("deprecatedT_deprecationRestatedF")
public fun deprecatedT_deprecationRestatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationRestatedF()
}

@ExportedBridge("deprecatedT_deprecationRestatedT_init_allocate")
public fun deprecatedT_deprecationRestatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationRestatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationRestatedT())
}

@ExportedBridge("deprecatedT_deprecationRestatedV_get")
public fun deprecatedT_deprecationRestatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationRestatedV
}

@ExportedBridge("normalChildT_deprecatedF")
public fun normalChildT_deprecatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.deprecatedF()
}

@ExportedBridge("normalChildT_deprecatedInFutureF")
public fun normalChildT_deprecatedInFutureF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.deprecatedInFutureF()
}

@ExportedBridge("normalChildT_deprecatedInFutureP_get")
public fun normalChildT_deprecatedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = __self.deprecatedInFutureP
    return _result
}

@ExportedBridge("normalChildT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    __self.deprecatedInFutureP = __newValue
}

@ExportedBridge("normalChildT_deprecatedInFutureV_get")
public fun normalChildT_deprecatedInFutureV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.deprecatedInFutureV
}

@ExportedBridge("normalChildT_deprecatedP_get")
public fun normalChildT_deprecatedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = __self.deprecatedP
    return _result
}

@ExportedBridge("normalChildT_deprecatedP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    __self.deprecatedP = __newValue
}

@ExportedBridge("normalChildT_deprecatedV_get")
public fun normalChildT_deprecatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.deprecatedV
}

@ExportedBridge("normalChildT_normalF")
public fun normalChildT_normalF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.normalF()
}

@ExportedBridge("normalChildT_normalV_get")
public fun normalChildT_normalV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.normalV
}

@ExportedBridge("normalChildT_obsoletedF")
public fun normalChildT_obsoletedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.obsoletedF()
}

@ExportedBridge("normalChildT_obsoletedInFutureF")
public fun normalChildT_obsoletedInFutureF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.obsoletedInFutureF()
}

@ExportedBridge("normalChildT_obsoletedInFutureP_get")
public fun normalChildT_obsoletedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = __self.obsoletedInFutureP
    return _result
}

@ExportedBridge("normalChildT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    __self.obsoletedInFutureP = __newValue
}

@ExportedBridge("normalChildT_obsoletedInFutureV_get")
public fun normalChildT_obsoletedInFutureV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.obsoletedInFutureV
}

@ExportedBridge("normalChildT_obsoletedP_get")
public fun normalChildT_obsoletedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = __self.obsoletedP
    return _result
}

@ExportedBridge("normalChildT_obsoletedP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    __self.obsoletedP = __newValue
}

@ExportedBridge("normalChildT_obsoletedV_get")
public fun normalChildT_obsoletedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.obsoletedV
}

@ExportedBridge("normalChildT_removedF")
public fun normalChildT_removedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.removedF()
}

@ExportedBridge("normalChildT_removedV_get")
public fun normalChildT_removedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    __self.removedV
}

@ExportedBridge("normalT_deprecatedF")
public fun normalT_deprecatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.deprecatedF()
}

@ExportedBridge("normalT_deprecatedInFutureF")
public fun normalT_deprecatedInFutureF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.deprecatedInFutureF()
}

@ExportedBridge("normalT_deprecatedInFutureP_get")
public fun normalT_deprecatedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = __self.deprecatedInFutureP
    return _result
}

@ExportedBridge("normalT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    __self.deprecatedInFutureP = __newValue
}

@ExportedBridge("normalT_deprecatedInFutureV_get")
public fun normalT_deprecatedInFutureV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.deprecatedInFutureV
}

@ExportedBridge("normalT_deprecatedP_get")
public fun normalT_deprecatedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = __self.deprecatedP
    return _result
}

@ExportedBridge("normalT_deprecatedP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    __self.deprecatedP = __newValue
}

@ExportedBridge("normalT_deprecatedT_init_allocate")
public fun normalT_deprecatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT.deprecatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, deprecated: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __deprecated = deprecated
    kotlin.native.internal.initInstance(____kt, normalT.deprecatedT(__deprecated))
}

@ExportedBridge("normalT_deprecatedV_get")
public fun normalT_deprecatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.deprecatedV
}

@ExportedBridge("normalT_normalF")
public fun normalT_normalF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.normalF()
}

@ExportedBridge("normalT_normalP_get")
public fun normalT_normalP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = __self.normalP
    return _result
}

@ExportedBridge("normalT_normalP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_normalP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    __self.normalP = __newValue
}

@ExportedBridge("normalT_normalT_init_allocate")
public fun normalT_normalT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT.normalT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_normalT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun normalT_normalT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, normalT.normalT())
}

@ExportedBridge("normalT_normalV_get")
public fun normalT_normalV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.normalV
}

@ExportedBridge("normalT_obsoletedF")
public fun normalT_obsoletedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.obsoletedF()
}

@ExportedBridge("normalT_obsoletedInFutureF")
public fun normalT_obsoletedInFutureF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.obsoletedInFutureF()
}

@ExportedBridge("normalT_obsoletedInFutureP_get")
public fun normalT_obsoletedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = __self.obsoletedInFutureP
    return _result
}

@ExportedBridge("normalT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    __self.obsoletedInFutureP = __newValue
}

@ExportedBridge("normalT_obsoletedInFutureV_get")
public fun normalT_obsoletedInFutureV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.obsoletedInFutureV
}

@ExportedBridge("normalT_obsoletedP_get")
public fun normalT_obsoletedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = __self.obsoletedP
    return _result
}

@ExportedBridge("normalT_obsoletedP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    __self.obsoletedP = __newValue
}

@ExportedBridge("normalT_obsoletedV_get")
public fun normalT_obsoletedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.obsoletedV
}

@ExportedBridge("normalT_removedInFutureF")
public fun normalT_removedInFutureF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.removedInFutureF()
}

@ExportedBridge("normalT_removedInFutureP_get")
public fun normalT_removedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = __self.removedInFutureP
    return _result
}

@ExportedBridge("normalT_removedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_removedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    __self.removedInFutureP = __newValue
}

@ExportedBridge("normalT_removedInFutureV_get")
public fun normalT_removedInFutureV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.removedInFutureV
}

