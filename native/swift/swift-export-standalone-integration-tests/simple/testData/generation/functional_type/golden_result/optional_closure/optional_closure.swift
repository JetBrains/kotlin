@_implementationOnly import KotlinBridges_optional_closure
import KotlinRuntime
import KotlinRuntimeSupport

public protocol MyInterface: KotlinRuntime.KotlinBase, optional_closure._MyInterface {
    func foo(
        arg: (() -> Swift.Void)?
    ) -> Swift.Void
}
@objc(_MyInterface)
public protocol _MyInterface {
}
public protocol __MyInterface: KotlinRuntimeSupport._KotlinBridgeable {
}
public func consume_consuming_opt_closure(
    arg: (((() -> Swift.String)?) -> Swift.Void)?
) -> Swift.Void {
    return { __root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___(arg.map { it in {
        let originalBlock: (Swift.Optional<() -> Swift.String>) -> Swift.Void = it
        return { (arg0: Swift.UnsafeMutableRawPointer?) in
            let _arg0: Swift.Optional<() -> Swift.String> = arg0.map { it in {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)!
                return { return optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!) }
            }() }
            let _result = originalBlock(_arg0)
            return { _result; return true }()
        }
    }() } ?? nil); return () }()
}
public func consume_opt_closure(
    arg: (() -> Swift.Void)?
) -> Swift.Void {
    return { __root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(arg.map { it in {
        let originalBlock: () -> Swift.Void = it
        return {
            let _result = originalBlock()
            return { _result; return true }()
        }
    }() } ?? nil); return () }()
}
public func consume_producing_opt_closure(
    arg: (() -> (() -> Swift.Void)?)?
) -> Swift.Void {
    return { __root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____(arg.map { it in {
        let originalBlock: () -> Swift.Optional<() -> Swift.Void> = it
        return {
            let _result = originalBlock()
            return _result.map { it in {
                let originalBlock: () -> Swift.Void = it
                return {
                    let _result = originalBlock()
                    return { _result; return true }()
                }
            }() } ?? nil
        }
    }() } ?? nil); return () }()
}
public func produce_opt_closure(
    arg: Swift.Void
) -> (() -> Swift.String)? {
    return __root___produce_opt_closure__TypesOfArguments__Swift_Void__({ arg; return true }()).map { it in {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)!
        return { return optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!) }
    }() }
}
extension optional_closure.MyInterface where Self : optional_closure.__MyInterface {
    public func foo(
        arg: (() -> Swift.Void)?
    ) -> Swift.Void {
        return { MyInterface_foo__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(self.__externalRCRef(), arg.map { it in {
            let originalBlock: () -> Swift.Void = it
            return {
                let _result = originalBlock()
                return { _result; return true }()
            }
        }() } ?? nil); return () }()
    }
}
extension optional_closure.MyInterface {
}
extension KotlinRuntimeSupport._KotlinExistential: optional_closure.MyInterface, optional_closure.__MyInterface where Wrapped : optional_closure._MyInterface {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: optional_closure._MyInterface {
}
