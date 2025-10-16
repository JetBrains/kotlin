@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.ranges.ClosedRange where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var endInclusive: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_ClosedRange_endInclusive_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public var start: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_ClosedRange_start_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public func contains(
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__(self.__externalRCRef(), value.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty(self.__externalRCRef())
    }
    public static func ~=(
        this: Self,
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        this.contains(value: value)
    }
}
extension ExportedKotlinPackages.kotlin.ranges.ClosedRange {
}
extension ExportedKotlinPackages.kotlin.Comparable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public static func <(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
    public func _compareTo(
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension ExportedKotlinPackages.kotlin.Comparable {
}
extension ExportedKotlinPackages.kotlin.ranges.OpenEndRange where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var endExclusive: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_OpenEndRange_endExclusive_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public var start: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_OpenEndRange_start_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public func contains(
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        return kotlin_ranges_OpenEndRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__(self.__externalRCRef(), value.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_OpenEndRange_isEmpty(self.__externalRCRef())
    }
    public static func ~=(
        this: Self,
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        this.contains(value: value)
    }
}
extension ExportedKotlinPackages.kotlin.ranges.OpenEndRange {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.ranges.ClosedRange where Wrapped : ExportedKotlinPackages.kotlin.ranges._ClosedRange {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.ranges.OpenEndRange where Wrapped : ExportedKotlinPackages.kotlin.ranges._OpenEndRange {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Comparable where Wrapped : ExportedKotlinPackages.kotlin._Comparable {
}
extension ExportedKotlinPackages.kotlin.collections {
    open class IntIterator: KotlinRuntime.KotlinBase {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public final func next() -> Swift.Int32 {
            return kotlin_collections_IntIterator_next(self.__externalRCRef())
        }
        open func nextInt() -> Swift.Int32 {
            return kotlin_collections_IntIterator_nextInt(self.__externalRCRef())
        }
    }
}
extension ExportedKotlinPackages.kotlin {
    public protocol Comparable: KotlinRuntime.KotlinBase {
        func _compareTo(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
    }
    @objc(_Comparable)
    package protocol _Comparable {
    }
}
extension ExportedKotlinPackages.kotlin.ranges {
    public protocol ClosedRange: KotlinRuntime.KotlinBase {
        var endInclusive: any ExportedKotlinPackages.kotlin.Comparable {
            get
        }
        var start: any ExportedKotlinPackages.kotlin.Comparable {
            get
        }
        func contains(
            value: any ExportedKotlinPackages.kotlin.Comparable
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    public protocol OpenEndRange: KotlinRuntime.KotlinBase {
        var endExclusive: any ExportedKotlinPackages.kotlin.Comparable {
            get
        }
        var start: any ExportedKotlinPackages.kotlin.Comparable {
            get
        }
        func contains(
            value: any ExportedKotlinPackages.kotlin.Comparable
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    @objc(_ClosedRange)
    package protocol _ClosedRange {
    }
    @objc(_OpenEndRange)
    package protocol _OpenEndRange {
    }
    open class IntProgression: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.IntProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.IntProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_IntProgression_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
            public func fromClosedRange(
                rangeStart: Swift.Int32,
                rangeEnd: Swift.Int32,
                step: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.ranges.IntProgression {
                return ExportedKotlinPackages.kotlin.ranges.IntProgression.__createClassWrapper(externalRCRef: kotlin_ranges_IntProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_Int32__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
        }
        public final var first: Swift.Int32 {
            get {
                return kotlin_ranges_IntProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.Int32 {
            get {
                return kotlin_ranges_IntProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int32 {
            get {
                return kotlin_ranges_IntProgression_step_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.IntProgression,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        open func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_IntProgression_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        open func hashCode() -> Swift.Int32 {
            return kotlin_ranges_IntProgression_hashCode(self.__externalRCRef())
        }
        open func isEmpty() -> Swift.Bool {
            return kotlin_ranges_IntProgression_isEmpty(self.__externalRCRef())
        }
        open func iterator() -> ExportedKotlinPackages.kotlin.collections.IntIterator {
            return ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: kotlin_ranges_IntProgression_iterator(self.__externalRCRef()))
        }
        open func toString() -> Swift.String {
            return kotlin_ranges_IntProgression_toString(self.__externalRCRef())
        }
    }
    public final class IntRange: ExportedKotlinPackages.kotlin.ranges.IntProgression {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.IntRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: kotlin_ranges_IntRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.IntRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.IntRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_IntRange_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with Int type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.Int32 {
            get {
                return kotlin_ranges_IntRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.Int32 {
            get {
                return kotlin_ranges_IntRange_endInclusive_get(self.__externalRCRef())
            }
        }
        public var start: Swift.Int32 {
            get {
                return kotlin_ranges_IntRange_start_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public init(
            start: Swift.Int32,
            endInclusive: Swift.Int32
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.ranges.IntRange.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.ranges.IntRange ") }
            let __kt = kotlin_ranges_IntRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_ranges_IntRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(__kt, start, endInclusive)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.ranges.IntRange,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func contains(
            value: Swift.Int32
        ) -> Swift.Bool {
            return kotlin_ranges_IntRange_contains__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value)
        }
        public override func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlin_ranges_IntRange_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public override func hashCode() -> Swift.Int32 {
            return kotlin_ranges_IntRange_hashCode(self.__externalRCRef())
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_IntRange_isEmpty(self.__externalRCRef())
        }
        public override func toString() -> Swift.String {
            return kotlin_ranges_IntRange_toString(self.__externalRCRef())
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.IntRange,
            value: Swift.Int32
        ) -> Swift.Bool {
            this.contains(value: value)
        }
    }
}
