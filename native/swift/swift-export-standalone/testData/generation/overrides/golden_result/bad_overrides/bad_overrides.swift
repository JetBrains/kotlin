@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_bad_overrides
import KotlinRuntime

public extension ExportedKotlinPackages.weird {
    open class A: KotlinRuntime.KotlinBase {
        open var bar: Swift.Int32 {
            get {
                return weird_A_bar_get(self.__externalRCRef())
            }
        }
        @_nonoverride
        public init() throws {
            let __kt = weird_A_init_allocate()
            super.init(__externalRCRef: __kt)
            struct KotlinError: Error { var wrapped: KotlinRuntime.KotlinBase }
            var __error: UInt = 0
            weird_A_init_initialize__TypesOfArguments__Swift_UInt__(__kt, &__error)
            guard __error == 0 else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase(__externalRCRef: __error)) }
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        @available(*, unavailable, message: "")
        open func foo() -> Swift.Void {
            return weird_A_foo(self.__externalRCRef())
        }
        open func `throws`() throws -> Swift.Void {
            struct KotlinError: Error { var wrapped: KotlinRuntime.KotlinBase }
            var _out_error: UInt = 0
            let _result = weird_A_throws(self.__externalRCRef(), &_out_error)
            guard _out_error == 0 else { throw KotlinError(wrapped: KotlinRuntime.KotlinBase(__externalRCRef: _out_error)) }
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
            weird_B_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
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
