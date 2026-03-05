@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public typealias AliasedAsyncFunctionType = (Swift.Float) async throws -> Swift.Int64
public typealias AliasedFunctionType = (Swift.Float) -> Swift.Int32
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
public var flowFoo: KotlinCoroutineSupport._KotlinTypedFlow<main.Foo> {
    get {
        return KotlinCoroutineSupport._KotlinTypedFlow<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowFoo_get()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
}
public func demo() -> KotlinCoroutineSupport._KotlinTypedFlow<main.Foo> {
    return KotlinCoroutineSupport._KotlinTypedFlow<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___demo()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func produce_flow() -> KotlinCoroutineSupport._KotlinTypedFlow<Swift.Int32> {
    return KotlinCoroutineSupport._KotlinTypedFlow<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produce_flow()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func produce_function() -> (Swift.Int32) async throws -> Swift.Int32 {
    return {
        let pointerToBlock = __root___produce_function()
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

                    let _: Bool = main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock, _1, {
                        let originalBlock = continuation
                        return { arg0 in return { originalBlock(arg0); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
                        return { arg0 in return { originalBlock({
                        let pointerToBlock = arg0
                        return { _1 in return main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock, _1) }
                    }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
                        return { arg0 in return { originalBlock({
                        let pointerToBlock = arg0
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

                                    let _: Bool = main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double__(pointerToBlock, _1, {
                                        let originalBlock = continuation
                                        return { arg0 in return { originalBlock(arg0); return true }() }
                                    }(), {
                                        let originalBlock = exception
                                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
                        return { arg0 in return { originalBlock({
                        let pointerToBlock = arg0
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

                                    let _: Bool = main_internal_functional_type_caller_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock, _1, {
                                        let originalBlock = continuation
                                        return { arg0 in return { originalBlock(arg0); return true }() }
                                    }(), {
                                        let originalBlock = exception
                                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
        let pointerToBlock = __root___returnSuspendUnit()
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

                    let _: Bool = main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock, {
                        let originalBlock = continuation
                        return { arg0 in return { originalBlock({ arg0; return () }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
                        return { arg0 in return { originalBlock({ arg0; return () }()); return true }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
