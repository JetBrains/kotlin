@_implementationOnly import KotlinBridges_optional_closure
import KotlinRuntime
import KotlinRuntimeSupport

public protocol MyInterface: KotlinRuntime.KotlinBase, optional_closure._MyInterface {
    func foo(
        arg: (() -> Swift.Void)?
    ) -> Swift.Void
}
@objc(_optional_closure_MyInterface)
public protocol _MyInterface {
}
public protocol __MyInterface: KotlinRuntimeSupport._KotlinBridgeable {
}
public func consume_consuming_opt_closure(
    arg: (((() -> Swift.String)?) -> Swift.Void)?
) -> Swift.Void {
    return { __root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___(arg.map { it in Unmanaged.passRetained((it as (Swift.Optional<() -> Swift.String>) -> Swift.Void) as AnyObject).toOpaque() } ?? nil); return () }()
}
public func consume_opt_closure(
    arg: (() -> Swift.Void)?
) -> Swift.Void {
    return { __root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(arg.map { it in Unmanaged.passRetained((it as () -> Swift.Void) as AnyObject).toOpaque() } ?? nil); return () }()
}
public func consume_producing_opt_closure(
    arg: (() -> (() -> Swift.Void)?)?
) -> Swift.Void {
    return { __root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____(arg.map { it in Unmanaged.passRetained((it as () -> Swift.Optional<() -> Swift.Void>) as AnyObject).toOpaque() } ?? nil); return () }()
}
public func produce_opt_closure(
    arg: Swift.Void
) -> (() -> Swift.String)? {
    return __root___produce_opt_closure__TypesOfArguments__Swift_Void__({ arg; return true }()).map { it in {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)
        return { return optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()) }
    }() }
}
@_documentation(visibility: internal)
extension optional_closure.MyInterface where Self : optional_closure.__MyInterface {
    public func foo(
        arg: (() -> Swift.Void)?
    ) -> Swift.Void {
        return { MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(self.__externalRCRef(), arg.map { it in Unmanaged.passRetained((it as () -> Swift.Void) as AnyObject).toOpaque() } ?? nil); return () }()
    }
}
extension optional_closure.MyInterface {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: optional_closure.MyInterface, optional_closure.__MyInterface where Wrapped : optional_closure._MyInterface {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: optional_closure._MyInterface {
}
@_cdecl("MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void_____reverse_swift")
package func MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: optional_closure.MyInterface.Type.self) as! any optional_closure.MyInterface
    let _result: Swift.Void = _self.foo(arg: arg.map { it in {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)
    return { return { optional_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()); return () }() }
}() })
    return { _result; return true }()
}

@_cdecl("optional_closure_internal_functional_type_callee_SwiftU2EOptionalU3C2829202D3E20SwiftU2EVoidU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func optional_closure_internal_functional_type_callee_SwiftU2EOptionalU3C2829202D3E20SwiftU2EVoidU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<() -> Swift.Void> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Optional<() -> Swift.Void>)()
    return _result.map { it in Unmanaged.passRetained((it as () -> Swift.Void) as AnyObject).toOpaque() } ?? nil
}

@_cdecl("optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_U2829202D_U20Swift_String___")
package func optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_U2829202D_U20Swift_String___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<() -> Swift.String>) -> Swift.Void)(_1.map { it in {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)
    return { return optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()) }
}() })
    return { _result; return true }()
}

@_cdecl("optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func optional_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Void)()
    return { _result; return true }()
}
