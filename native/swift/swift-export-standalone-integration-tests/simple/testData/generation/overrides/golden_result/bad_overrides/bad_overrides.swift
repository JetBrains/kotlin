@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_bad_overrides
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.weird {
    open class A: KotlinRuntime.KotlinBase {
        open var bar: Swift.Int32 {
            get {
                if Self.self == ExportedKotlinPackages.weird.A.self {
                    return weird_A_bar_get(self.__externalRCRef())
                } else {
                    return weird_A_bar_get_direct(self.__externalRCRef())
                }
            }
        }
        public init() throws {
            let __kt = weird_A_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            var __error: UnsafeMutableRawPointer? = nil
            weird_A_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt, &__error)
            try KotlinRuntimeSupport.raiseKotlinError(__error)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @available(*, unavailable, message: "")
        open func foo() -> Swift.Void {
            fatalError()
        }
        open func `throws`() throws -> Swift.Void {
            if Self.self == ExportedKotlinPackages.weird.A.self {
                var _out_error: UnsafeMutableRawPointer? = nil
                let _result = weird_A_throws(self.__externalRCRef(), &_out_error)
                try KotlinRuntimeSupport.raiseKotlinError(_out_error)
                return { _result; return () }()
            } else {
                var _out_error: UnsafeMutableRawPointer? = nil
                let _result = weird_A_throws_direct(self.__externalRCRef(), &_out_error)
                try KotlinRuntimeSupport.raiseKotlinError(_out_error)
                return { _result; return () }()
            }
        }
    }
    public final class B: ExportedKotlinPackages.weird.A {
        @_nonoverride
        public var bar: Swift.Never {
            get {
                return { weird_B_bar_get(self.__externalRCRef()); fatalError() }()
            }
        }
        @_nonoverride
        public init() {
            let __kt = weird_B_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { weird_B_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public func foo() -> Swift.Void {
            return { weird_B_foo(self.__externalRCRef()); return () }()
        }
        @_nonoverride
        public func `throws`() -> Swift.Void {
            return { weird_B_throws(self.__externalRCRef()); return () }()
        }
    }
}
@_cdecl("weird_A_bar_get__reverse_swift")
package func weird_A_bar_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.weird.A.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.bar
    return _result
}

@_cdecl("weird_A_throws__reverse_swift")
package func weird_A_throws__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ _out_error: Swift.UnsafeMutablePointer<Swift.UnsafeMutableRawPointer?>) -> Swift.Bool {
    let _self = ExportedKotlinPackages.weird.A.__createClassWrapper(externalRCRef: `self`)!
    do {
        let _result: Swift.Void = try _self.throws()
        return { _result; return true }()
    } catch {
        _out_error.pointee = KotlinRuntimeSupport.kotlinThrowableRCRef(for: error)
        return false
    }
}
