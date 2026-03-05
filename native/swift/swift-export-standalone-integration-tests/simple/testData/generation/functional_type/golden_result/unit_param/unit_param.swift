@_implementationOnly import KotlinBridges_unit_param
import KotlinRuntime
import KotlinRuntimeSupport

public func bar() -> (Swift.String, Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = __root___bar()
        return { _1, _2 in return { unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(pointerToBlock, _1, { _2; return true }()); return () }() }
    }()
}
public func barIn(
    block: @escaping (Swift.String, Swift.Void) -> Swift.Void
) -> Swift.Void {
    return { __root___barIn__TypesOfArguments__U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__({
        let originalBlock = block
        return { arg0, arg1 in return { originalBlock(arg0, { arg1; return () }()); return true }() }
    }()); return () }()
}
public func baz() -> (@escaping (Swift.String, Swift.Void) -> Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = __root___baz()
        return { _1 in return { unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__(pointerToBlock, {
        let originalBlock = _1
        return { arg0, arg1 in return { originalBlock(arg0, { arg1; return () }()); return true }() }
    }()); return () }() }
    }()
}
public func foo() -> (Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = __root___foo()
        return { _1 in return { unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock, { _1; return true }()); return () }() }
    }()
}
public func fooIn(
    block: @escaping (Swift.Void) -> Swift.Void
) -> Swift.Void {
    return { __root___fooIn__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Void__({
        let originalBlock = block
        return { arg0 in return { originalBlock({ arg0; return () }()); return true }() }
    }()); return () }()
}
