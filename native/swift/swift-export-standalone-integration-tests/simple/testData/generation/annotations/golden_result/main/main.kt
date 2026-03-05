@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinObjectB::class, "4main11ObjCObjectBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinClassA::class, "4main11SwiftClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinClassA.KotlinSubClassC::class, "4main11SwiftClassAC13ObjCSubClassCC")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinClassA.KotlinSubClassA::class, "4main11SwiftClassAC14SwiftSubClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinClassA.KotlinSubClassB::class, "4main11SwiftClassAC14SwiftSubClassBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinClassA.KotlinSubClassD::class, "4main11SwiftClassAC14SwiftSubClassDC")
@file:kotlin.native.internal.objc.BindClassToObjCName(KotlinInterfaceC::class, "_SwiftInterfaceC")
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

@ExportedBridge("KotlinClassA_KotlinSubClassA_init_allocate")
public fun KotlinClassA_KotlinSubClassA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<KotlinClassA.KotlinSubClassA>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("KotlinClassA_KotlinSubClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun KotlinClassA_KotlinSubClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, KotlinClassA.KotlinSubClassA()) }
    return run { _result; true }
}

@ExportedBridge("KotlinClassA_KotlinSubClassB_init_allocate")
public fun KotlinClassA_KotlinSubClassB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<KotlinClassA.KotlinSubClassB>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("KotlinClassA_KotlinSubClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun KotlinClassA_KotlinSubClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, KotlinClassA.KotlinSubClassB()) }
    return run { _result; true }
}

@ExportedBridge("KotlinClassA_KotlinSubClassC_init_allocate")
public fun KotlinClassA_KotlinSubClassC_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<KotlinClassA.KotlinSubClassC>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("KotlinClassA_KotlinSubClassC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun KotlinClassA_KotlinSubClassC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, KotlinClassA.KotlinSubClassC()) }
    return run { _result; true }
}

@ExportedBridge("KotlinClassA_KotlinSubClassD_init_allocate")
public fun KotlinClassA_KotlinSubClassD_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<KotlinClassA.KotlinSubClassD>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("KotlinClassA_KotlinSubClassD_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun KotlinClassA_KotlinSubClassD_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, KotlinClassA.KotlinSubClassD()) }
    return run { _result; true }
}

@ExportedBridge("KotlinClassA_kotlinFunA__TypesOfArguments__Swift_String__")
public fun KotlinClassA_kotlinFunA__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, swiftParamA: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinClassA
    val __swiftParamA = interpretObjCPointer<kotlin.String>(swiftParamA)
    val _result = run { __self.kotlinFunA(__swiftParamA) }
    return run { _result; true }
}

@ExportedBridge("KotlinClassA_kotlinPropA_get")
public fun KotlinClassA_kotlinPropA_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinClassA
    val _result = run { __self.kotlinPropA }
    return _result.objcPtr()
}

@ExportedBridge("KotlinClassA_kotlinPropB_get")
public fun KotlinClassA_kotlinPropB_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinClassA
    val _result = run { __self.kotlinPropB }
    return _result.objcPtr()
}

@ExportedBridge("KotlinClassA_kotlinPropB_set__TypesOfArguments__Swift_String__")
public fun KotlinClassA_kotlinPropB_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinClassA
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.kotlinPropB = __newValue }
    return run { _result; true }
}

@ExportedBridge("KotlinInterfaceC_kotlinFunD__TypesOfArguments__Swift_String__")
public fun KotlinInterfaceC_kotlinFunD__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, swiftParamD: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinInterfaceC
    val __swiftParamD = interpretObjCPointer<kotlin.String>(swiftParamD)
    val _result = run { __self.kotlinFunD(__swiftParamD) }
    return run { _result; true }
}

@ExportedBridge("KotlinInterfaceC_kotlinFunE__TypesOfArguments__Swift_String__")
public fun KotlinInterfaceC_kotlinFunE__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, kotlinParamE: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinInterfaceC
    val __kotlinParamE = interpretObjCPointer<kotlin.String>(kotlinParamE)
    val _result = run { __self.kotlinFunE(__kotlinParamE) }
    return run { _result; true }
}

@ExportedBridge("KotlinObjectB_kotlinFunB__TypesOfArguments__Swift_String__")
public fun KotlinObjectB_kotlinFunB__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, objCParamB: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinObjectB
    val __objCParamB = interpretObjCPointer<kotlin.String>(objCParamB)
    val _result = run { __self.kotlinFunB(__objCParamB) }
    return run { _result; true }
}

@ExportedBridge("KotlinObjectB_kotlinFunC__TypesOfArguments__Swift_String__")
public fun KotlinObjectB_kotlinFunC__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, objCParamC: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as KotlinObjectB
    val __objCParamC = interpretObjCPointer<kotlin.String>(objCParamC)
    val _result = run { __self.kotlinFunC(__objCParamC) }
    return run { _result; true }
}

@ExportedBridge("__root___KotlinClassA_init_allocate")
public fun __root___KotlinClassA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<KotlinClassA>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___KotlinClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___KotlinClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, KotlinClassA()) }
    return run { _result; true }
}

@ExportedBridge("__root___KotlinObjectB_get")
public fun __root___KotlinObjectB_get(): kotlin.native.internal.NativePtr {
    val _result = run { KotlinObjectB }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MESSAGE_get")
public fun __root___MESSAGE_get(): kotlin.native.internal.NativePtr {
    val _result = run { MESSAGE }
    return _result.objcPtr()
}

@ExportedBridge("__root___classA_get")
public fun __root___classA_get(): kotlin.native.internal.NativePtr {
    val _result = run { classA }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___constMessage")
public fun __root___constMessage(): kotlin.native.internal.NativePtr {
    val _result = run { constMessage() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedChildT_init_allocate")
public fun __root___deprecatedChildT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<deprecatedChildT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___deprecatedChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, deprecatedChildT()) }
    return run { _result; true }
}

@ExportedBridge("__root___deprecatedF")
public fun __root___deprecatedF(): Boolean {
    val _result = run { deprecatedF() }
    return run { _result; true }
}

@ExportedBridge("__root___deprecatedImplicitlyF")
public fun __root___deprecatedImplicitlyF(): Boolean {
    val _result = run { deprecatedImplicitlyF() }
    return run { _result; true }
}

@ExportedBridge("__root___deprecatedT_init_allocate")
public fun __root___deprecatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<deprecatedT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, deprecatedT()) }
    return run { _result; true }
}

@ExportedBridge("__root___deprecationInheritedImplicitlyV_get")
public fun __root___deprecationInheritedImplicitlyV_get(): Boolean {
    val _result = run { deprecationInheritedImplicitlyV }
    return run { _result; true }
}

@ExportedBridge("__root___deprecationInheritedV_get")
public fun __root___deprecationInheritedV_get(): Boolean {
    val _result = run { deprecationInheritedV }
    return run { _result; true }
}

@ExportedBridge("__root___formattedMessage")
public fun __root___formattedMessage(): kotlin.native.internal.NativePtr {
    val _result = run { formattedMessage() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___interfaceC_get")
public fun __root___interfaceC_get(): kotlin.native.internal.NativePtr {
    val _result = run { interfaceC }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___multilineFormattedMessage")
public fun __root___multilineFormattedMessage(): kotlin.native.internal.NativePtr {
    val _result = run { multilineFormattedMessage() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___multilineMessage")
public fun __root___multilineMessage(): kotlin.native.internal.NativePtr {
    val _result = run { multilineMessage() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalChildT_init_allocate")
public fun __root___normalChildT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<normalChildT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___normalChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, normalChildT()) }
    return run { _result; true }
}

@ExportedBridge("__root___normalT_init_allocate")
public fun __root___normalT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<normalT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, normalT()) }
    return run { _result; true }
}

@ExportedBridge("__root___objectB_get")
public fun __root___objectB_get(): kotlin.native.internal.NativePtr {
    val _result = run { objectB }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___obsoletedF")
public fun __root___obsoletedF(): Boolean {
    val _result = run { obsoletedF() }
    return run { _result; true }
}

@ExportedBridge("__root___obsoletedV_get")
public fun __root___obsoletedV_get(): Boolean {
    val _result = run { obsoletedV }
    return run { _result; true }
}

@ExportedBridge("__root___renamed__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamed__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = run { renamed(__x, __y) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedF")
public fun __root___renamedF(): Boolean {
    val _result = run { renamedF() }
    return run { _result; true }
}

@ExportedBridge("__root___renamedQualified__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamedQualified__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = run { renamedQualified(__x, __y) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedQualifiedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamedQualifiedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = run { renamedQualifiedWithArguments(__x, __y) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedT_init_allocate")
public fun __root___renamedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<renamedT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___renamedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, renamedT()) }
    return run { _result; true }
}

@ExportedBridge("__root___renamedV_get")
public fun __root___renamedV_get(): Boolean {
    val _result = run { renamedV }
    return run { _result; true }
}

@ExportedBridge("__root___renamedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun __root___renamedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x: Int, y: Float): kotlin.native.internal.NativePtr {
    val __x = x
    val __y = y
    val _result = run { renamedWithArguments(__x, __y) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnClassA__TypesOfArguments__main_SwiftClassA__")
public fun __root___returnClassA__TypesOfArguments__main_SwiftClassA__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as KotlinClassA
    val _result = run { returnClassA(__value) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnInterfaceC__TypesOfArguments__anyU20main_SwiftInterfaceC__")
public fun __root___returnInterfaceC__TypesOfArguments__anyU20main_SwiftInterfaceC__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as KotlinInterfaceC
    val _result = run { returnInterfaceC(__value) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnObjectB__TypesOfArguments__main_ObjCObjectB__")
public fun __root___returnObjectB__TypesOfArguments__main_ObjCObjectB__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as KotlinObjectB
    val _result = run { returnObjectB(__value) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___unrenamed")
public fun __root___unrenamed(): kotlin.native.internal.NativePtr {
    val _result = run { unrenamed() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedChildT_deprecationFurtherReinforcedF")
public fun deprecatedChildT_deprecationFurtherReinforcedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    val _result = run { __self.deprecationFurtherReinforcedF() }
    return run { _result; true }
}

@ExportedBridge("deprecatedChildT_deprecationFurtherReinforcedV_get")
public fun deprecatedChildT_deprecationFurtherReinforcedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    val _result = run { __self.deprecationFurtherReinforcedV }
    return run { _result; true }
}

@ExportedBridge("deprecatedChildT_deprecationReinforcedF")
public fun deprecatedChildT_deprecationReinforcedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    val _result = run { __self.deprecationReinforcedF() }
    return run { _result; true }
}

@ExportedBridge("deprecatedChildT_deprecationReinforcedV_get")
public fun deprecatedChildT_deprecationReinforcedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    val _result = run { __self.deprecationReinforcedV }
    return run { _result; true }
}

@ExportedBridge("deprecatedChildT_deprecationRestatedF")
public fun deprecatedChildT_deprecationRestatedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    val _result = run { __self.deprecationRestatedF() }
    return run { _result; true }
}

@ExportedBridge("deprecatedChildT_deprecationRestatedV_get")
public fun deprecatedChildT_deprecationRestatedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedChildT
    val _result = run { __self.deprecationRestatedV }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationInheritedF")
public fun deprecatedT_deprecationInheritedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    val _result = run { __self.deprecationInheritedF() }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationInheritedT_init_allocate")
public fun deprecatedT_deprecationInheritedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationInheritedT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationInheritedT()) }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationInheritedV_get")
public fun deprecatedT_deprecationInheritedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    val _result = run { __self.deprecationInheritedV }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationReinforcedF")
public fun deprecatedT_deprecationReinforcedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    val _result = run { __self.deprecationReinforcedF() }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationReinforcedV_get")
public fun deprecatedT_deprecationReinforcedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    val _result = run { __self.deprecationReinforcedV }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationRestatedF")
public fun deprecatedT_deprecationRestatedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    val _result = run { __self.deprecationRestatedF() }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationRestatedT_init_allocate")
public fun deprecatedT_deprecationRestatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationRestatedT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationRestatedT()) }
    return run { _result; true }
}

@ExportedBridge("deprecatedT_deprecationRestatedV_get")
public fun deprecatedT_deprecationRestatedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    val _result = run { __self.deprecationRestatedV }
    return run { _result; true }
}

@ExportedBridge("normalChildT_deprecatedF")
public fun normalChildT_deprecatedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.deprecatedF() }
    return run { _result; true }
}

@ExportedBridge("normalChildT_deprecatedInFutureF")
public fun normalChildT_deprecatedInFutureF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.deprecatedInFutureF() }
    return run { _result; true }
}

@ExportedBridge("normalChildT_deprecatedInFutureP_get")
public fun normalChildT_deprecatedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.deprecatedInFutureP }
    return _result
}

@ExportedBridge("normalChildT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    val _result = run { __self.deprecatedInFutureP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalChildT_deprecatedInFutureV_get")
public fun normalChildT_deprecatedInFutureV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.deprecatedInFutureV }
    return run { _result; true }
}

@ExportedBridge("normalChildT_deprecatedP_get")
public fun normalChildT_deprecatedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.deprecatedP }
    return _result
}

@ExportedBridge("normalChildT_deprecatedP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    val _result = run { __self.deprecatedP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalChildT_deprecatedV_get")
public fun normalChildT_deprecatedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.deprecatedV }
    return run { _result; true }
}

@ExportedBridge("normalChildT_normalF")
public fun normalChildT_normalF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.normalF() }
    return run { _result; true }
}

@ExportedBridge("normalChildT_normalV_get")
public fun normalChildT_normalV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.normalV }
    return run { _result; true }
}

@ExportedBridge("normalChildT_obsoletedF")
public fun normalChildT_obsoletedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.obsoletedF() }
    return run { _result; true }
}

@ExportedBridge("normalChildT_obsoletedInFutureF")
public fun normalChildT_obsoletedInFutureF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.obsoletedInFutureF() }
    return run { _result; true }
}

@ExportedBridge("normalChildT_obsoletedInFutureP_get")
public fun normalChildT_obsoletedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.obsoletedInFutureP }
    return _result
}

@ExportedBridge("normalChildT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    val _result = run { __self.obsoletedInFutureP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalChildT_obsoletedInFutureV_get")
public fun normalChildT_obsoletedInFutureV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.obsoletedInFutureV }
    return run { _result; true }
}

@ExportedBridge("normalChildT_obsoletedP_get")
public fun normalChildT_obsoletedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.obsoletedP }
    return _result
}

@ExportedBridge("normalChildT_obsoletedP_set__TypesOfArguments__Swift_Int32__")
public fun normalChildT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val __newValue = newValue
    val _result = run { __self.obsoletedP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalChildT_obsoletedV_get")
public fun normalChildT_obsoletedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.obsoletedV }
    return run { _result; true }
}

@ExportedBridge("normalChildT_removedF")
public fun normalChildT_removedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.removedF() }
    return run { _result; true }
}

@ExportedBridge("normalChildT_removedV_get")
public fun normalChildT_removedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalChildT
    val _result = run { __self.removedV }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedF")
public fun normalT_deprecatedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.deprecatedF() }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedInFutureF")
public fun normalT_deprecatedInFutureF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.deprecatedInFutureF() }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedInFutureP_get")
public fun normalT_deprecatedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.deprecatedInFutureP }
    return _result
}

@ExportedBridge("normalT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    val _result = run { __self.deprecatedInFutureP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedInFutureV_get")
public fun normalT_deprecatedInFutureV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.deprecatedInFutureV }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedP_get")
public fun normalT_deprecatedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.deprecatedP }
    return _result
}

@ExportedBridge("normalT_deprecatedP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    val _result = run { __self.deprecatedP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedT_init_allocate")
public fun normalT_deprecatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<normalT.deprecatedT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, deprecated: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __deprecated = deprecated
    val _result = run { kotlin.native.internal.initInstance(____kt, normalT.deprecatedT(__deprecated)) }
    return run { _result; true }
}

@ExportedBridge("normalT_deprecatedV_get")
public fun normalT_deprecatedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.deprecatedV }
    return run { _result; true }
}

@ExportedBridge("normalT_normalF")
public fun normalT_normalF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.normalF() }
    return run { _result; true }
}

@ExportedBridge("normalT_normalP_get")
public fun normalT_normalP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.normalP }
    return _result
}

@ExportedBridge("normalT_normalP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_normalP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    val _result = run { __self.normalP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalT_normalT_init_allocate")
public fun normalT_normalT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<normalT.normalT>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun normalT_normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, normalT.normalT()) }
    return run { _result; true }
}

@ExportedBridge("normalT_normalV_get")
public fun normalT_normalV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.normalV }
    return run { _result; true }
}

@ExportedBridge("normalT_obsoletedF")
public fun normalT_obsoletedF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.obsoletedF() }
    return run { _result; true }
}

@ExportedBridge("normalT_obsoletedInFutureF")
public fun normalT_obsoletedInFutureF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.obsoletedInFutureF() }
    return run { _result; true }
}

@ExportedBridge("normalT_obsoletedInFutureP_get")
public fun normalT_obsoletedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.obsoletedInFutureP }
    return _result
}

@ExportedBridge("normalT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    val _result = run { __self.obsoletedInFutureP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalT_obsoletedInFutureV_get")
public fun normalT_obsoletedInFutureV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.obsoletedInFutureV }
    return run { _result; true }
}

@ExportedBridge("normalT_obsoletedP_get")
public fun normalT_obsoletedP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.obsoletedP }
    return _result
}

@ExportedBridge("normalT_obsoletedP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    val _result = run { __self.obsoletedP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalT_obsoletedV_get")
public fun normalT_obsoletedV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.obsoletedV }
    return run { _result; true }
}

@ExportedBridge("normalT_removedInFutureF")
public fun normalT_removedInFutureF(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.removedInFutureF() }
    return run { _result; true }
}

@ExportedBridge("normalT_removedInFutureP_get")
public fun normalT_removedInFutureP_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.removedInFutureP }
    return _result
}

@ExportedBridge("normalT_removedInFutureP_set__TypesOfArguments__Swift_Int32__")
public fun normalT_removedInFutureP_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val __newValue = newValue
    val _result = run { __self.removedInFutureP = __newValue }
    return run { _result; true }
}

@ExportedBridge("normalT_removedInFutureV_get")
public fun normalT_removedInFutureV_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    val _result = run { __self.removedInFutureV }
    return run { _result; true }
}
