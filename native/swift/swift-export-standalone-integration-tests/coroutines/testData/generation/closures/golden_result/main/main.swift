@_implementationOnly import KotlinBridges_main
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func accept_suspend_fun_with_context(
    block: @escaping (Swift.String) async throws -> Swift.Int32
) -> Swift.Int32 {
    return __root___accept_suspend_fun_with_context__TypesOfArguments__U28Swift_StringU2920asyncU20throwsU202D_U20Swift_Int32__(Unmanaged.passRetained((block as (Swift.String) async throws -> Swift.Int32) as AnyObject).toOpaque())
}
public func accept_suspend_function_type(
    block: @escaping () async throws -> Swift.Int32
) -> Swift.Int32 {
    return __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__(Unmanaged.passRetained((block as () async throws -> Swift.Int32) as AnyObject).toOpaque())
}
@_cdecl("main_internal_functional_type_callee_async_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
package func main_internal_functional_type_callee_async_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ ctx0: Swift.String, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
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
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.String) async throws -> Swift.Int32)((ctx0))
    }
    return true
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
