#if canImport(FooKit)
import FooKit
#endif
import Foundation
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class RegularKotlinClass: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___RegularKotlinClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___RegularKotlinClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public func consumeCopyable(
    x: any Foundation.NSCopying
) -> Swift.Void {
    return { __root___consumeCopyable__TypesOfArguments__anyU20Foundation_NSCopying__(x); return () }()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func consumePlain(
    x: Swift.Never
) -> Swift.Void {
    fatalError()
}
public func produceCopyable() -> any Foundation.NSCopying {
    return __root___produceCopyable() as! any Foundation.NSCopying
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func producePlain() -> Swift.Never {
    fatalError()
}
public func produceZar() -> any Zar {
    return __root___produceZar() as! any Zar
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func produceZarImpl() -> Swift.Never {
    fatalError()
}
// Can't export PlainNSObjectSubclass: Kotlin subclasses of Objective-C classes are not supported.
// Can't export CopyableImpl: Kotlin subclasses of Objective-C classes are not supported.
// Can't export ZarImpl: Kotlin subclasses of Objective-C classes are not supported.
// Can't export PlainNSObjectSubclass: Kotlin subclasses of Objective-C classes are not supported.
// Can't export PlainNSObjectSubclass: Kotlin subclasses of Objective-C classes are not supported.
// Can't export ZarImpl: Kotlin subclasses of Objective-C classes are not supported.
