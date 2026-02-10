@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public typealias AliasedFunctionType = (Swift.Float) -> Swift.Int32
public final class Foo: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public var flowFoo: KotlinCoroutineSupport._KotlinTypedFlow<main.Foo> {
    get {
        return KotlinCoroutineSupport._KotlinTypedFlow<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___flowFoo_get()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
}
public func closure_returning_flow(
    i: @escaping (KotlinCoroutineSupport._KotlinTypedFlow<main.Foo>) -> Swift.Void
) -> Swift.Void {
    return __root___closure_returning_flow__TypesOfArguments__U28KotlinCoroutineSupport__KotlinTypedFlow_main_Foo_U29202D_U20Swift_Void__({
        let originalBlock = i
        return { arg0 in return originalBlock(KotlinCoroutineSupport._KotlinTypedFlow<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)) }
    }())
}
public func demo() -> KotlinCoroutineSupport._KotlinTypedFlow<main.Foo> {
    return KotlinCoroutineSupport._KotlinTypedFlow<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___demo()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
}
public func demo_ft_produce() -> (Swift.Int32) async throws -> Swift.Int32 {
    return {
        let pointerToBlock = __root___demo_ft_produce()
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

                    let _: () = main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock, _1, {
                        let originalBlock = continuation
                        return { arg0 in return { originalBlock(arg0); return 0 }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return 0 }() }
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

                    let _: () = __root___produce_function_typealias({
                        let originalBlock = continuation
                        return { arg0 in return { originalBlock({
                        let pointerToBlock = arg0
                        return { _1 in return main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock, _1) }
                    }()); return 0 }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return 0 }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }()
}
