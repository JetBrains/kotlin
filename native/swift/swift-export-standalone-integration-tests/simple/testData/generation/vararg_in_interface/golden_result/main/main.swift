@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Driver: KotlinRuntime.KotlinBase, main._Driver {
    func addListener(
        queryKeys: Swift.String...,
        listener: any main._Driver_Listener
    ) -> Swift.Void
}
@objc(_Driver)
public protocol _Driver {
}
public protocol _Driver_Listener: KotlinRuntime.KotlinBase, main.__Driver_Listener {
}
public protocol __Driver: KotlinRuntimeSupport._KotlinBridgeable {
}
@objc(__Driver_Listener)
public protocol __Driver_Listener {
}
public protocol ___Driver_Listener: KotlinRuntimeSupport._KotlinBridgeable {
}
open class BaseDriver: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___BaseDriver_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___BaseDriver_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
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
extension main.Driver where Self : main.__Driver {
    public func addListener(
        queryKeys: Swift.String...,
        listener: any main._Driver_Listener
    ) -> Swift.Void {
        return { Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener__(self.__externalRCRef(), queryKeys, listener.__externalRCRef()); return () }()
    }
}
extension main.Driver {
    typealias Listener = main._Driver_Listener
}
extension main._Driver_Listener where Self : main.___Driver_Listener {
}
extension main._Driver_Listener {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Driver, main.__Driver where Wrapped : main._Driver {
}
extension KotlinRuntimeSupport._KotlinExistential: main._Driver_Listener, main.___Driver_Listener where Wrapped : main.__Driver_Listener {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Driver {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main.__Driver_Listener {
}
@_cdecl("BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift")
package func BaseDriver_addInts__TypesOfArguments__Swift_Array_Swift_Int32__Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any) -> Swift.Bool {
    let _self = main.BaseDriver.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = unsafeBitCast(_self.addInts, to: ((Swift.Array<Swift.Int32>) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.Int32>)
    return { _result; return true }()
}

@_cdecl("BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift")
package func BaseDriver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any) -> Swift.Bool {
    let _self = main.BaseDriver.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = unsafeBitCast(_self.addListener, to: ((Swift.Array<Swift.String>) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.String>)
    return { _result; return true }()
}

@_cdecl("BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift")
package func BaseDriver_addOptionalInts__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32___Vararg_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any) -> Swift.Bool {
    let _self = main.BaseDriver.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = unsafeBitCast(_self.addOptionalInts, to: ((Swift.Array<Swift.Optional<Swift.Int32>>) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.Optional<Swift.Int32>>)
    return { _result; return true }()
}

@_cdecl("Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift")
package func Driver_addListener__TypesOfArguments__Swift_Array_Swift_String__Vararg__anyU20main__Driver_Listener____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ queryKeys: Any, _ listener: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.Driver
    let _result: Swift.Void = unsafeBitCast(_self.addListener, to: ((Swift.Array<Swift.String>, any main._Driver_Listener) -> Swift.Void).self)(queryKeys as! Swift.Array<Swift.String>, KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: listener) as! any main._Driver_Listener)
    return { _result; return true }()
}
