@_implementationOnly import KotlinBridges_main
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public protocol AsyncGreeter: KotlinRuntime.KotlinBase, main._AsyncGreeter {
    func greet(
        name: Swift.String
    ) async throws -> Swift.String
    func salutation() async throws -> Swift.String
}
@objc(_AsyncGreeter)
public protocol _AsyncGreeter {
}
public protocol __AsyncGreeter: KotlinRuntimeSupport._KotlinBridgeable {
}
open class AsyncAbstractBase: KotlinRuntime.KotlinBase {
    package init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func abstractGreet() async throws -> Swift.String {
        if Self.self == main.AsyncAbstractBase.self {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncAbstractBase_abstractGreet(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        } else {
            fatalError("Cannot invoke the inherited implementation of abstract member 'main.AsyncAbstractBase.abstractGreet': a Swift subclass must override it and must not call super.")
        }
    }
    open func concreteGreet() async throws -> Swift.String {
        if Self.self == main.AsyncAbstractBase.self {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncAbstractBase_concreteGreet(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        } else {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncAbstractBase_concreteGreet_direct(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        }
    }
}
open class AsyncBase: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___AsyncBase_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___AsyncBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func count() async throws -> Swift.Int32 {
        if Self.self == main.AsyncBase.self {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncBase_count(self.__externalRCRef(), {
                let originalBlock: (Swift.Int32) -> Swift.Void = continuation
                return { (arg0: Swift.Int32) in
                    let _arg0: Swift.Int32 = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        } else {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncBase_count_direct(self.__externalRCRef(), {
                let originalBlock: (Swift.Int32) -> Swift.Void = continuation
                return { (arg0: Swift.Int32) in
                    let _arg0: Swift.Int32 = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        }
    }
    open func greet(
        name: Swift.String
    ) async throws -> Swift.String {
        if Self.self == main.AsyncBase.self {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncBase_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name, {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        } else {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncBase_greet__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name, {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        }
    }
    public final func notOpen() async throws -> Swift.String {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncBase_notOpen(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    open func sync(
        name: Swift.String
    ) -> Swift.String {
        if Self.self == main.AsyncBase.self {
            return AsyncBase_sync__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
        } else {
            return AsyncBase_sync__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name)
        }
    }
}
open class AsyncGreeterBase: KotlinRuntime.KotlinBase, main.AsyncGreeter, main.__AsyncGreeter {
    public init() {
        let __kt = __root___AsyncGreeterBase_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___AsyncGreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func greet(
        name: Swift.String
    ) async throws -> Swift.String {
        if Self.self == main.AsyncGreeterBase.self {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncGreeterBase_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name, {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        } else {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncGreeterBase_greet__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name, {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        }
    }
    open func salutation() async throws -> Swift.String {
        if Self.self == main.AsyncGreeterBase.self {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncGreeterBase_salutation(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        } else {
            try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncGreeterBase_salutation_direct(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
        }
    }
}
extension main.AsyncGreeter where Self : main.__AsyncGreeter {
    public func greet(
        name: Swift.String
    ) async throws -> Swift.String {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncGreeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name, {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func salutation() async throws -> Swift.String {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = AsyncGreeter_salutation(self.__externalRCRef(), {
                let originalBlock: (Swift.String) -> Swift.Void = continuation
                return { (arg0: Swift.String) in
                    let _arg0: Swift.String = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
}
extension main.AsyncGreeter {
}
extension KotlinRuntimeSupport._KotlinExistential: main.AsyncGreeter, main.__AsyncGreeter where Wrapped : main._AsyncGreeter {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._AsyncGreeter {
}
@_cdecl("AsyncAbstractBase_abstractGreet__reverse_swift")
package func AsyncAbstractBase_abstractGreet__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.AsyncAbstractBase.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.abstractGreet()
    }
    return true
}

@_cdecl("AsyncAbstractBase_concreteGreet__reverse_swift")
package func AsyncAbstractBase_concreteGreet__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.AsyncAbstractBase.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.concreteGreet()
    }
    return true
}

@_cdecl("AsyncBase_count__reverse_swift")
package func AsyncBase_count__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.AsyncBase.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.Int32) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.count()
    }
    return true
}

@_cdecl("AsyncBase_greet__TypesOfArguments__Swift_String____reverse_swift")
package func AsyncBase_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.AsyncBase.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.greet(name: name)
    }
    return true
}

@_cdecl("AsyncBase_sync__TypesOfArguments__Swift_String____reverse_swift")
package func AsyncBase_sync__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = main.AsyncBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.sync(name: name)
    return _result
}

@_cdecl("AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift")
package func AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.AsyncGreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.greet(name: name)
    }
    return true
}

@_cdecl("AsyncGreeterBase_salutation__reverse_swift")
package func AsyncGreeterBase_salutation__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.AsyncGreeterBase.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.salutation()
    }
    return true
}

@_cdecl("AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse_swift")
package func AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.AsyncGreeter
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.greet(name: name)
    }
    return true
}

@_cdecl("AsyncGreeter_salutation__reverse_swift")
package func AsyncGreeter_salutation__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any main.AsyncGreeter
    let __continuation: (Swift.String) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.salutation()
    }
    return true
}
