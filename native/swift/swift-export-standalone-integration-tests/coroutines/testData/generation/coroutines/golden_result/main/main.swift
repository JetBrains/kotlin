@_implementationOnly import KotlinBridges_main
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public typealias AliasedAsyncFunctionType = (Swift.Float) async throws -> Swift.Int64
public typealias AliasedFunctionType = (Swift.Float) -> Swift.Int32
public protocol FunctionalInterfaceWithSuspendFunction: KotlinRuntime.KotlinBase, main._FunctionalInterfaceWithSuspendFunction {
    func emit() async throws -> Swift.Void
}
@objc(_main_FunctionalInterfaceWithSuspendFunction)
public protocol _FunctionalInterfaceWithSuspendFunction {
}
public protocol __FunctionalInterfaceWithSuspendFunction: KotlinRuntimeSupport._KotlinBridgeable {
}
public final class Foo: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public var flowFoo: any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo> {
    get {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<main.Foo>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowFoo_get(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, main.Foo.Type.self)
    }
}
public func accept_suspend_function_type(
    block: @escaping () async throws -> Swift.Int32
) -> Swift.Void {
    return { __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__(Unmanaged.passRetained((block as () async throws -> Swift.Int32) as AnyObject).toOpaque()); return () }()
}
public func alwaysFails() async throws -> Swift.Never {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___alwaysFails(Unmanaged.passRetained((continuation as (Swift.Never) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func closure_returning_flow(
    i: @escaping (any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo>) -> Swift.Void
) -> Swift.Void {
    return { __root___closure_returning_flow__TypesOfArguments__U28anyU20KotlinCoroutineSupport_KotlinTypedFlow_main_Foo_U29202D_U20Swift_Void__(Unmanaged.passRetained((i as (any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func consume_flow(
    flow: any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo>
) -> Swift.Void {
    return { __root___consume_flow__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_main_Foo___(flow.wrapped.__externalRCRef()); return () }()
}
public func demo() -> any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<main.Foo>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___demo(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, main.Foo.Type.self)
}
public func flowOfNullableUnit() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowOfNullableUnit(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
}
public func flowOfUnit() -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowOfUnit(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
}
public func functionalInterfaceWithSuspendFunction(
    function: @escaping () async throws -> Swift.Void
) -> any main.FunctionalInterfaceWithSuspendFunction {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___FunctionalInterfaceWithSuspendFunction__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Void__(Unmanaged.passRetained((function as () async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: main.FunctionalInterfaceWithSuspendFunction.Type.self) as! any main.FunctionalInterfaceWithSuspendFunction
}
public func mutableStateFlowOfUnit() -> any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
    return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___mutableStateFlowOfUnit(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
}
public func produce_flow() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int32> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produce_flow(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, Swift.Int32.Type.self)
}
public func produce_function() -> (Swift.Int32) async throws -> Swift.Int32 {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___produce_function(), options: .asBestFittingWrapper)
        return { _1 in try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = main_internal_functional_type_caller_async_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef(), _1, Unmanaged.passRetained((continuation as (Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    } }
    }()
}
public func produce_function_typealias() async throws -> main.AliasedFunctionType {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___produce_function_typealias(Unmanaged.passRetained((continuation as (@escaping (Swift.Float) -> Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func produce_suspend_function() async throws -> (Swift.Double) async throws -> Swift.Int32 {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___produce_suspend_function(Unmanaged.passRetained((continuation as (@escaping (Swift.Double) async throws -> Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func produce_suspend_function_typealias() async throws -> main.AliasedAsyncFunctionType {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___produce_suspend_function_typealias(Unmanaged.passRetained((continuation as (@escaping (Swift.Float) async throws -> Swift.Int64) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func retunsListOfSuspend() async throws -> [() async throws -> Swift.Void] {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___retunsListOfSuspend(Unmanaged.passRetained((continuation as (Swift.Array<() async throws -> Swift.Void>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func returnSuspendGeneric() async throws -> any KotlinRuntimeSupport._KotlinBridgeable {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___returnSuspendGeneric(Unmanaged.passRetained((continuation as (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func returnSuspendUnit() -> () async throws -> Swift.Void {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___returnSuspendUnit(), options: .asBestFittingWrapper)
        return { try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = main_internal_functional_type_caller_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    } }
    }()
}
public func returnUnit() async throws -> Swift.Void {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___returnUnit(Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func returnsList() async throws -> [Swift.String] {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___returnsList(Unmanaged.passRetained((continuation as (Swift.Array<Swift.String>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func returnsListOfSuspendNullables() async throws -> [(() async throws -> Swift.Void)?] {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = __root___returnsListOfSuspendNullables(Unmanaged.passRetained((continuation as (Swift.Array<Swift.Optional<() async throws -> Swift.Void>>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension main.FunctionalInterfaceWithSuspendFunction where Self : main.__FunctionalInterfaceWithSuspendFunction {
    public func emit() async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = FunctionalInterfaceWithSuspendFunction_emit(self.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
}
extension main.FunctionalInterfaceWithSuspendFunction {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.FunctionalInterfaceWithSuspendFunction, main.__FunctionalInterfaceWithSuspendFunction where Wrapped : main._FunctionalInterfaceWithSuspendFunction {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._FunctionalInterfaceWithSuspendFunction {
}
@_cdecl("FunctionalInterfaceWithSuspendFunction_emit__reverse_swift")
package func FunctionalInterfaceWithSuspendFunction_emit__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.FunctionalInterfaceWithSuspendFunction.Type.self) as! any main.FunctionalInterfaceWithSuspendFunction
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.emit()
    }
    return true
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Optional_U282920asyncU20throwsU202D_U20Swift_Void____")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Optional_U282920asyncU20throwsU202D_U20Swift_Void____(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Array<Swift.Optional<() async throws -> Swift.Void>>) -> Swift.Void)((_1 as! [Any]).map { __element in { let __v = __element as! Swift.Int; return __v == 0 ? nil : {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: Swift.UnsafeMutableRawPointer(bitPattern: __v)!, options: .asBestFittingWrapper)
    return { try await withKotlinContinuation { continuation, exception, cancellation in
    let _: Bool = main_internal_functional_type_caller_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
} }
}() }() })
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_String___")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_String___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Array<Swift.String>) -> Swift.Void)(_1 as! Swift.Array<Swift.String>)
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_U282920asyncU20throwsU202D_U20Swift_Void___")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_U282920asyncU20throwsU202D_U20Swift_Void___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Array<() async throws -> Swift.Void>) -> Swift.Void)((_1 as! [Any]).map { __element in {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: Swift.UnsafeMutableRawPointer(bitPattern: __element as! Swift.Int)!, options: .asBestFittingWrapper)
    return { try await withKotlinContinuation { continuation, exception, cancellation in
    let _: Bool = main_internal_functional_type_caller_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
} }
}() })
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int32) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int32) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.Error>) -> Swift.Void)({ switch _1 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)); } }())
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Bool) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Void) -> Swift.Void)({ _1; return () }())
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_DoubleU2920asyncU20throwsU202D_U20Swift_Int32__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_DoubleU2920asyncU20throwsU202D_U20Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (@escaping (Swift.Double) async throws -> Swift.Int32) -> Swift.Void)({
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: _1, options: .asBestFittingWrapper)
    return { _1 in try await withKotlinContinuation { continuation, exception, cancellation in
    let _: Bool = main_internal_functional_type_caller_async_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double__(pointerToBlock.__externalRCRef(), _1, Unmanaged.passRetained((continuation as (Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
} }
}())
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_FloatU29202D_U20Swift_Int32__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_FloatU29202D_U20Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (@escaping (Swift.Float) -> Swift.Int32) -> Swift.Void)({
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: _1, options: .asBestFittingWrapper)
    return { _1 in return main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock.__externalRCRef(), _1) }
}())
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_FloatU2920asyncU20throwsU202D_U20Swift_Int64__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_FloatU2920asyncU20throwsU202D_U20Swift_Int64__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (@escaping (Swift.Float) async throws -> Swift.Int64) -> Swift.Void)({
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: _1, options: .asBestFittingWrapper)
    return { _1 in try await withKotlinContinuation { continuation, exception, cancellation in
    let _: Bool = main_internal_functional_type_caller_async_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock.__externalRCRef(), _1, Unmanaged.passRetained((continuation as (Swift.Int64) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
} }
}())
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_main_Foo___")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_main_Foo___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo>) -> Swift.Void)(KotlinCoroutineSupport._KotlinTypedFlowImpl<main.Foo>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, main.Foo.Type.self))
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
package func main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void)(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: _1))
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_async_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func main_internal_functional_type_callee_async_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Int32) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () async throws -> Swift.Int32)()
    }
    return true
}

@_cdecl("main_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func main_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () async throws -> Swift.Void)()
    }
    return true
}
