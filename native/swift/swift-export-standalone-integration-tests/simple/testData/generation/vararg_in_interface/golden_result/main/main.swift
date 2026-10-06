@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Driver: KotlinRuntime.KotlinBase, main._Driver {
    func addListener(
        queryKeys: Swift.String...,
        listener: any main._Driver_Listener
    ) -> Swift.Void
}
public protocol ExtensionVararg: KotlinRuntime.KotlinBase, main._ExtensionVararg {
    func join(
        _ receiver: Swift.String,
        parts: Swift.String...
    ) -> Swift.String
}
public protocol KeywordLabels: KotlinRuntime.KotlinBase, main._KeywordLabels {
    func count(
        `inout`: Swift.Int32...
    ) -> Swift.Int32
    func double(
        `inout`: Swift.Int32
    ) -> Swift.Int32
}
@objc(_main_Driver)
public protocol _Driver {
}
public protocol _Driver_Listener: KotlinRuntime.KotlinBase, main.__Driver_Listener {
}
@objc(_main_ExtensionVararg)
public protocol _ExtensionVararg {
}
@objc(_main_KeywordLabels)
public protocol _KeywordLabels {
}
public protocol __Driver: KotlinRuntimeSupport._KotlinBridgeable {
}
@objc(_main__Driver_Listener)
public protocol __Driver_Listener {
}
public protocol __ExtensionVararg: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __KeywordLabels: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol ___Driver_Listener: KotlinRuntimeSupport._KotlinBridgeable {
}
open class BaseDriver: KotlinRuntime.KotlinBase {
    public override init() {
         let __kt: Swift.UnsafeMutableRawPointer!
         if Self.self == main.BaseDriver.self {
             __kt = __root___BaseDriver_init_allocate()
         } else {
             __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
         }
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___BaseDriver_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func addInts(
        queryKeys: Swift.Int32...
    ) -> Swift.Void {
        if Self.self == main.BaseDriver.self {
            return { BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___(self.__externalRCRef(), queryKeys.map { it in NSNumber(value: it) }); return () }()
        } else {
            return { BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg____direct(self.__externalRCRef(), queryKeys.map { it in NSNumber(value: it) }); return () }()
        }
    }
    open func addListener(
        queryKeys: Swift.String...
    ) -> Swift.Void {
        if Self.self == main.BaseDriver.self {
            return { BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg___(self.__externalRCRef(), queryKeys); return () }()
        } else {
            return { BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg____direct(self.__externalRCRef(), queryKeys); return () }()
        }
    }
    open func addOptionalInts(
        queryKeys: Swift.Int32?...
    ) -> Swift.Void {
        if Self.self == main.BaseDriver.self {
            return { BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg___(self.__externalRCRef(), queryKeys.map { it in it as! NSObject? ?? NSNull() }); return () }()
        } else {
            return { BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg____direct(self.__externalRCRef(), queryKeys.map { it in it as! NSObject? ?? NSNull() }); return () }()
        }
    }
}
@_documentation(visibility: internal)
extension main.Driver where Self : main.__Driver {
    public func addListener(
        queryKeys: Swift.String...,
        listener: any main._Driver_Listener
    ) -> Swift.Void {
        return { Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener__(self.__externalRCRef(), queryKeys, listener.__externalRCRef()); return () }()
    }
}
extension main.Driver {
    public typealias Listener = main._Driver_Listener
}
@_documentation(visibility: internal)
extension main.ExtensionVararg where Self : main.__ExtensionVararg {
    public func join(
        _ receiver: Swift.String,
        parts: Swift.String...
    ) -> Swift.String {
        return ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg___(self.__externalRCRef(), receiver, parts)
    }
}
extension main.ExtensionVararg {
}
@_documentation(visibility: internal)
extension main.KeywordLabels where Self : main.__KeywordLabels {
    public func count(
        `inout`: Swift.Int32...
    ) -> Swift.Int32 {
        return KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg___(self.__externalRCRef(), `inout`.map { it in NSNumber(value: it) })
    }
    public func double(
        `inout`: Swift.Int32
    ) -> Swift.Int32 {
        return KeywordLabels_double__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), `inout`)
    }
}
extension main.KeywordLabels {
}
@_documentation(visibility: internal)
extension main._Driver_Listener where Self : main.___Driver_Listener {
}
extension main._Driver_Listener {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.Driver, main.__Driver where Wrapped : main._Driver {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.KeywordLabels, main.__KeywordLabels where Wrapped : main._KeywordLabels {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.ExtensionVararg, main.__ExtensionVararg where Wrapped : main._ExtensionVararg {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main._Driver_Listener, main.___Driver_Listener where Wrapped : main.__Driver_Listener {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Driver {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._KeywordLabels {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._ExtensionVararg {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main.__Driver_Listener {
}
@_cdecl("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift")
package func BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any) -> Swift.Bool {
    let _self = main.BaseDriver.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = unsafeBitCast(_self.addInts(queryKeys:), to: ((Swift.Array<Swift.Int32>) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.Int32>)
    return { _result; return true }()
}

@_cdecl("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift")
package func BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any) -> Swift.Bool {
    let _self = main.BaseDriver.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = unsafeBitCast(_self.addListener(queryKeys:), to: ((Swift.Array<Swift.String>) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.String>)
    return { _result; return true }()
}

@_cdecl("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift")
package func BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any) -> Swift.Bool {
    let _self = main.BaseDriver.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = unsafeBitCast(_self.addOptionalInts(queryKeys:), to: ((Swift.Array<Swift.Optional<Swift.Int32>>) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.Optional<Swift.Int32>>)
    return { _result; return true }()
}

@_cdecl("Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift")
package func Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any, _ listener: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.Driver.Type.self) as! any main.Driver
    let _result: Swift.Void = unsafeBitCast(_self.addListener(queryKeys:listener:), to: ((Swift.Array<Swift.String>, any main._Driver_Listener) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.String>, KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: listener, conformsTo: main._Driver_Listener.Type.self) as! any main._Driver_Listener)
    return { _result; return true }()
}

@_cdecl("ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg_____reverse_swift")
package func ExtensionVararg_join__TypesOfArgumentsE__Swift_String_Swift_Array_Swift_String__Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.String, _ parts: Any) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.ExtensionVararg.Type.self) as! any main.ExtensionVararg
    let _result: Swift.String = unsafeBitCast(_self.join(_:parts:), to: ((Swift.String, Swift.Array<Swift.String>) -> Swift.String).self)(receiver, parts as! Swift.Array<Swift.String>)
    return _result
}

@_cdecl("KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift")
package func KeywordLabels_count__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ `inout`: Any) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.KeywordLabels.Type.self) as! any main.KeywordLabels
    let _result: Swift.Int32 = unsafeBitCast(_self.count(`inout`:), to: ((Swift.Array<Swift.Int32>) -> Swift.Int32).self)(`inout` as! Swift.Array<Swift.Int32>)
    return _result
}

@_cdecl("KeywordLabels_double__TypesOfArguments__Swift_Int32____reverse_swift")
package func KeywordLabels_double__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ `inout`: Swift.Int32) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.KeywordLabels.Type.self) as! any main.KeywordLabels
    let _result: Swift.Int32 = _self.double(`inout`: `inout`)
    return _result
}
