@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public typealias AliasedAsyncFunctionType = (Swift.Float) async throws -> Swift.Int64
public typealias AliasedFunctionType = (Swift.Float) -> Swift.Int32
public protocol FunctionalInterfaceWithSuspendFunction: KotlinRuntime.KotlinBase {
    func emit() async throws -> Swift.Void
}
@objc(_FunctionalInterfaceWithSuspendFunction)
package protocol _FunctionalInterfaceWithSuspendFunction {
}
public final class Foo: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public var flowFoo: any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo> {
    get {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowFoo_get()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
}
public func accept_suspend_function_type(
    block: @escaping () async throws -> Swift.Int32
) -> Swift.Void {
    return { __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__({
        let originalBlock = block
        return { (__continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
            let __continuation: (Swift.Int32) -> Swift.Void = {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1); return () }() }
    }()
            let __exception: (Swift.Error) -> Swift.Void = {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
    }()
            let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)

            let task = Task {
                await withTaskCancellationHandler {
                    do {
                        let result = try await originalBlock()
                        __continuation(result)
                    } catch {
                        __exception(error)
                    }
                } onCancel: {
                    __cancellation.cancelExternally()
                }
            }
            __cancellation.setCallback { shouldCancel in
                defer { if shouldCancel { task.cancel() } }
                return task.isCancelled
            }
        }
    }()); return () }()
}
public func alwaysFails() async throws -> Swift.Never {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Never) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___alwaysFails({
                        let originalBlock = continuation
                        return { (arg0: Swift.Bool) in return { originalBlock({ arg0; fatalError() }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func closure_returning_flow(
    i: @escaping (any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo>) -> Swift.Void
) -> Swift.Void {
    return { __root___closure_returning_flow__TypesOfArguments__U28anyU20KotlinCoroutineSupport_KotlinTypedFlow_main_Foo_U29202D_U20Swift_Void__({
        let originalBlock = i
        return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(KotlinCoroutineSupport._KotlinTypedFlowImpl<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)); return true }() }
    }()); return () }()
}
public func consume_flow(
    flow: any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo>
) -> Swift.Void {
    return { __root___consume_flow__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_main_Foo___(flow.wrapped.__externalRCRef()); return () }()
}
public func demo() -> any KotlinCoroutineSupport.KotlinTypedFlow<main.Foo> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___demo()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func flowOfNullableUnit() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowOfNullableUnit()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func flowOfUnit() -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowOfUnit()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func functionalInterfaceWithSuspendFunction(
    function: @escaping () async throws -> Swift.Void
) -> any main.FunctionalInterfaceWithSuspendFunction {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___FunctionalInterfaceWithSuspendFunction__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Void__({
        let originalBlock = function
        return { (__continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
            let __continuation: (Swift.Void) -> Swift.Void = {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
    }()
            let __exception: (Swift.Error) -> Swift.Void = {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
    }()
            let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)

            let task = Task {
                await withTaskCancellationHandler {
                    do {
                        let result = try await originalBlock()
                        __continuation(result)
                    } catch {
                        __exception(error)
                    }
                } onCancel: {
                    __cancellation.cancelExternally()
                }
            }
            __cancellation.setCallback { shouldCancel in
                defer { if shouldCancel { task.cancel() } }
                return task.isCancelled
            }
        }
    }())) as! any main.FunctionalInterfaceWithSuspendFunction
}
public func mutableStateFlowOfUnit() -> any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
    return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___mutableStateFlowOfUnit()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow)
}
public func produce_flow() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int32> {
    return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produce_flow()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func produce_function() -> (Swift.Int32) async throws -> Swift.Int32 {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___produce_function(), options: .asBestFittingWrapper)!
        return { _1 in try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1, {
                        let originalBlock = continuation
                        return { (arg0: Swift.Int32) in return { originalBlock(arg0); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }() }
    }()
}
public func produce_function_typealias() async throws -> main.AliasedFunctionType {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (@escaping (Swift.Float) -> Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___produce_function_typealias({
                        let originalBlock = continuation
                        return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock({
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: arg0, options: .asBestFittingWrapper)!
                        return { _1 in return main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock.__externalRCRef()!, _1) }
                    }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func produce_suspend_function() async throws -> (Swift.Double) async throws -> Swift.Int32 {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (@escaping (Swift.Double) async throws -> Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___produce_suspend_function({
                        let originalBlock = continuation
                        return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock({
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: arg0, options: .asBestFittingWrapper)!
                        return { _1 in try await {
                        try Task.checkCancellation()
                        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                        return try await withTaskCancellationHandler {
                            try await withUnsafeThrowingContinuation { nativeContinuation in
                                withUnsafeCurrentTask { currentTask in
                                    let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                                    }
                                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                                    let _: Bool = main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double__(pointerToBlock.__externalRCRef()!, _1, {
                                        let originalBlock = continuation
                                        return { (arg0: Swift.Int32) in return { originalBlock(arg0); return true }() }
                                    }(), {
                                        let originalBlock = exception
                                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                                    }(), cancellation.__externalRCRef())
                                }
                            }
                        } onCancel: {
                            cancellation?.cancelExternally()
                        }
                    }() }
                    }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func produce_suspend_function_typealias() async throws -> main.AliasedAsyncFunctionType {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (@escaping (Swift.Float) async throws -> Swift.Int64) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___produce_suspend_function_typealias({
                        let originalBlock = continuation
                        return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock({
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: arg0, options: .asBestFittingWrapper)!
                        return { _1 in try await {
                        try Task.checkCancellation()
                        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                        return try await withTaskCancellationHandler {
                            try await withUnsafeThrowingContinuation { nativeContinuation in
                                withUnsafeCurrentTask { currentTask in
                                    let continuation: (Swift.Int64) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                                    }
                                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                                    let _: Bool = main_internal_functional_type_caller_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock.__externalRCRef()!, _1, {
                                        let originalBlock = continuation
                                        return { (arg0: Swift.Int64) in return { originalBlock(arg0); return true }() }
                                    }(), {
                                        let originalBlock = exception
                                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                                    }(), cancellation.__externalRCRef())
                                }
                            }
                        } onCancel: {
                            cancellation?.cancelExternally()
                        }
                    }() }
                    }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func retunsListOfSuspend() async throws -> [() async throws -> Swift.Void] {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Array<() async throws -> Swift.Void>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___retunsListOfSuspend({
                        let originalBlock = continuation
                        return { (arg0: Any) in return { originalBlock((arg0 as! [Any]).map { __element in {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: Swift.UnsafeMutableRawPointer(bitPattern: __element as! Swift.Int)!, options: .asBestFittingWrapper)!
                        return { try await {
                        try Task.checkCancellation()
                        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                        return try await withTaskCancellationHandler {
                            try await withUnsafeThrowingContinuation { nativeContinuation in
                                withUnsafeCurrentTask { currentTask in
                                    let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                                    }
                                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                                    let _: Bool = main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!, {
                                        let originalBlock = continuation
                                        return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                                    }(), {
                                        let originalBlock = exception
                                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                                    }(), cancellation.__externalRCRef())
                                }
                            }
                        } onCancel: {
                            cancellation?.cancelExternally()
                        }
                    }() }
                    }() }); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func returnSuspendUnit() -> () async throws -> Swift.Void {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___returnSuspendUnit(), options: .asBestFittingWrapper)!
        return { try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!, {
                        let originalBlock = continuation
                        return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }() }
    }()
}
public func returnUnit() async throws -> Swift.Void {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___returnUnit({
                        let originalBlock = continuation
                        return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func returnsList() async throws -> [Swift.String] {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Array<Swift.String>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___returnsList({
                        let originalBlock = continuation
                        return { (arg0: Any) in return { originalBlock(arg0 as! Swift.Array<Swift.String>); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
public func returnsListOfSuspendNullables() async throws -> [(() async throws -> Swift.Void)?] {
    try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Array<Swift.Optional<() async throws -> Swift.Void>>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: Bool = __root___returnsListOfSuspendNullables({
                        let originalBlock = continuation
                        return { (arg0: Any) in return { originalBlock((arg0 as! [Any]).map { __element in { let __v = __element as! Swift.Int; return __v == 0 ? nil : {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: Swift.UnsafeMutableRawPointer(bitPattern: __v)!, options: .asBestFittingWrapper)!
                        return { try await {
                        try Task.checkCancellation()
                        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                        return try await withTaskCancellationHandler {
                            try await withUnsafeThrowingContinuation { nativeContinuation in
                                withUnsafeCurrentTask { currentTask in
                                    let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                                    }
                                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                                    let _: Bool = main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!, {
                                        let originalBlock = continuation
                                        return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                                    }(), {
                                        let originalBlock = exception
                                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                                    }(), cancellation.__externalRCRef())
                                }
                            }
                        } onCancel: {
                            cancellation?.cancelExternally()
                        }
                    }() }
                    }() }() }); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
extension main.FunctionalInterfaceWithSuspendFunction where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func emit() async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = FunctionalInterfaceWithSuspendFunction_emit(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
}
extension main.FunctionalInterfaceWithSuspendFunction {
}
extension KotlinRuntimeSupport._KotlinExistential: main.FunctionalInterfaceWithSuspendFunction where Wrapped : main._FunctionalInterfaceWithSuspendFunction {
}
