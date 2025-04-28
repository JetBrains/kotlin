@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_bad_overrides
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.weird {
    open class A: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        open var bar: Swift.Int32 {
            get {
                return weird_A_bar_get(self.__externalRCRef())
            }
        }
        public init() throws {
            precondition(Self.self == ExportedKotlinPackages.weird.A.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.weird.A ")
            let __kt = weird_A_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            var __error: UnsafeMutableRawPointer? = nil
            weird_A_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt, &__error)
            guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: __error)) }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        @available(*, unavailable, message: "")
        open func foo() -> Swift.Void {
            return weird_A_foo(self.__externalRCRef())
        }
        open func `throws`() throws -> Swift.Void {
            var _out_error: UnsafeMutableRawPointer? = nil
            let _result = weird_A_throws(self.__externalRCRef(), &_out_error)
            guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase.__create(externalRCRef: _out_error)) }
            return _result
        }
    }
    public final class B: ExportedKotlinPackages.weird.A {
        @_nonoverride
        public var bar: Swift.Never {
            get {
                return weird_B_bar_get(self.__externalRCRef())
            }
        }
        @_nonoverride
        public init() {
            precondition(Self.self == ExportedKotlinPackages.weird.B.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.weird.B ")
            let __kt = weird_B_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            weird_B_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func foo() -> Swift.Void {
            return weird_B_foo(self.__externalRCRef())
        }
        @_nonoverride
        public func `throws`() -> Swift.Void {
            return weird_B_throws(self.__externalRCRef())
        }
    }
}
