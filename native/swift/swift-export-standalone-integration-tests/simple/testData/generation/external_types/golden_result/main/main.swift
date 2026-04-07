import Foundation
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

@_spi(kotlinx$cinterop$ExperimentalForeignApi) @available(*, unavailable, message: "Declaration uses unsupported types")
public var store_cgReck: Swift.Never {
    @_spi(kotlinx$cinterop$ExperimentalForeignApi)
    get {
        fatalError()
    }
    @_spi(kotlinx$cinterop$ExperimentalForeignApi)
    set {
        fatalError()
    }
}
public var store_nsdate: Foundation.NSDate {
    get {
        return __root___store_nsdate_get() as! Foundation.NSDate
    }
    set {
        return { __root___store_nsdate_set__TypesOfArguments__Foundation_NSDate__(newValue); return () }()
    }
}
public func consume_nsdate(
    date: Foundation.NSDate
) -> Swift.Void {
    return { __root___consume_nsdate__TypesOfArguments__Foundation_NSDate__(date); return () }()
}
public func produce_nsdate() -> Foundation.NSDate {
    return __root___produce_nsdate() as! Foundation.NSDate
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func produce_typealias() -> Swift.Never {
    fatalError()
}
