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
        @_nonoverride
        public init() throws {
            let __kt = weird_A_init_allocate()
            super.init(__externalRCRef: __kt)
            var __error: UnsafeMutableRawPointer? = nil
            weird_A_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt, &__error)
            guard __error == .none else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase(__externalRCRef: __error)) }
        }
        package override init(
            __externalRCRef: Swift.UnsafeMutableRawPointer?
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        @available(*, unavailable, message: "")
        open func foo() -> Swift.Void {
            return weird_A_foo(self.__externalRCRef())
        }
        open func `throws`() throws -> Swift.Void {
            var _out_error: UnsafeMutableRawPointer? = nil
            let _result = weird_A_throws(self.__externalRCRef(), &_out_error)
            guard _out_error == nil else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase(__externalRCRef: _out_error)) }
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
            let __kt = weird_B_init_allocate()
            super.init(__externalRCRef: __kt)
            weird_B_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UnsafeMutableRawPointer?
        ) {
            super.init(__externalRCRef: __externalRCRef)
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
