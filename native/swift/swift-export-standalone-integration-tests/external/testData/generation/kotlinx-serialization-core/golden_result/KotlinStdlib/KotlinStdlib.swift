@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
@_implementationOnly import KotlinBridges_KotlinStdlib

public protocol _ExportedKotlinPackages_kotlin_collections_Map_Entry<K, V>: KotlinRuntime.KotlinBase, KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
    associatedtype K: KotlinRuntimeSupport._KotlinBridgeable
    associatedtype V: KotlinRuntimeSupport._KotlinBridgeable
    var key: K {
        get
    }
    var value: V {
        get
    }
}
@objc(_KotlinStdlib__ExportedKotlinPackages_kotlin_collections_Map_Entry)
public protocol __ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
public protocol ___ExportedKotlinPackages_kotlin_collections_Map_Entry: KotlinRuntimeSupport._KotlinBridgeable {
}
public final class _ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry, KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_Map_Entry, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
extension ExportedKotlinPackages.kotlin {
    public protocol Annotation: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin._Annotation {
    }
    @objc(_ExportedKotlinPackages_kotlin_Annotation)
    public protocol _Annotation {
    }
    public protocol __Annotation: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol CharSequence: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin._CharSequence {
        var length: Swift.Int32 {
            get
        }
        func _get(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit
        func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence
    }
    @objc(_ExportedKotlinPackages_kotlin_CharSequence)
    public protocol _CharSequence {
    }
    public protocol __CharSequence: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Comparable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin._Comparable {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func _compareTo(
            other: T
        ) -> Swift.Int32
    }
    @objc(_ExportedKotlinPackages_kotlin_Comparable)
    public protocol _Comparable {
    }
    public protocol __Comparable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class Array<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var size: Swift.Int32 {
            get {
                return _cbridge_kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        @inlinable
        public func _get(
            index: Swift.Int32
        ) -> T {
            return T.__fromKotlinPointer(_cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
        }
        @inlinable
        public func _set(
            index: Swift.Int32,
            value: T
        ) -> Swift.Void {
            return { _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(self.__externalRCRef(), index, value.__retainedToKotlinPointer()); return () }()
        }
        @inlinable
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
            return { let _ptr = _cbridge_kotlin_Array_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> T
        ) {
            fatalError()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public subscript(
            index: Swift.Int32
        ) -> T {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class ByteArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_ByteArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return { kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.ByteIterator {
            return ExportedKotlinPackages.kotlin.collections.ByteIterator.__createClassWrapper(externalRCRef: kotlin_ByteArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int8
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int8 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class CharArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_CharArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_CharArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return { kotlin_CharArray_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.CharIterator {
            return ExportedKotlinPackages.kotlin.collections.CharIterator.__createClassWrapper(externalRCRef: kotlin_CharArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Unicode.UTF16.CodeUnit
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class ShortArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_ShortArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int16 {
            return kotlin_ShortArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int16
        ) -> Swift.Void {
            return { kotlin_ShortArray_set__TypesOfArguments__Swift_Int32_Swift_Int16__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.ShortIterator {
            return ExportedKotlinPackages.kotlin.collections.ShortIterator.__createClassWrapper(externalRCRef: kotlin_ShortArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int16
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int16 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class IntArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_IntArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_IntArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return { kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.IntIterator {
            return ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: kotlin_IntArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int32 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class LongArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_LongArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_LongArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int64
        ) -> Swift.Void {
            return { kotlin_LongArray_set__TypesOfArguments__Swift_Int32_Swift_Int64__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.LongIterator {
            return ExportedKotlinPackages.kotlin.collections.LongIterator.__createClassWrapper(externalRCRef: kotlin_LongArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int64
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int64 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class FloatArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_FloatArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Float {
            return kotlin_FloatArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Float
        ) -> Swift.Void {
            return { kotlin_FloatArray_set__TypesOfArguments__Swift_Int32_Swift_Float__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.FloatIterator {
            return ExportedKotlinPackages.kotlin.collections.FloatIterator.__createClassWrapper(externalRCRef: kotlin_FloatArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Float
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Float {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class DoubleArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_DoubleArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Double {
            return kotlin_DoubleArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Double
        ) -> Swift.Void {
            return { kotlin_DoubleArray_set__TypesOfArguments__Swift_Int32_Swift_Double__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.DoubleIterator {
            return ExportedKotlinPackages.kotlin.collections.DoubleIterator.__createClassWrapper(externalRCRef: kotlin_DoubleArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Double
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Double {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class BooleanArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_BooleanArray_size_get(self.__externalRCRef())
            }
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlin_BooleanArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void {
            return { kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.BooleanIterator {
            return ExportedKotlinPackages.kotlin.collections.BooleanIterator.__createClassWrapper(externalRCRef: kotlin_BooleanArray_iterator(self.__externalRCRef()))
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Bool
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Bool {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class Boolean: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Bool
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.Boolean.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Boolean.Companion.__createClassWrapper(externalRCRef: kotlin_Boolean_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _not() -> Swift.Bool {
            return kotlin_Boolean_not(self.__externalRCRef())
        }
        public static prefix func !(
            this: ExportedKotlinPackages.kotlin.Boolean
        ) -> Swift.Bool {
            this._not()
        }
        public func and(
            other: Swift.Bool
        ) -> Swift.Bool {
            return kotlin_Boolean_and__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.Bool
        ) -> Swift.Bool {
            return kotlin_Boolean_or__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.Bool
        ) -> Swift.Bool {
            return kotlin_Boolean_xor__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public func _compareTo(
            other: Swift.Bool
        ) -> Swift.Int32 {
            return kotlin_Boolean_compareTo__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Boolean,
            other: Swift.Bool
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Char: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Unicode.UTF16.CodeUnit
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var MIN_HIGH_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_HIGH_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MAX_HIGH_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_HIGH_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MIN_LOW_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_LOW_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MAX_LOW_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_LOW_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MIN_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MIN_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var MAX_SURROGATE: Swift.Unicode.UTF16.CodeUnit {
                get {
                    return kotlin_Char_Companion_MAX_SURROGATE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            @_spi(kotlin$experimental$ExperimentalNativeApi)
            public var MIN_SUPPLEMENTARY_CODE_POINT: Swift.Int32 {
                @_spi(kotlin$experimental$ExperimentalNativeApi)
                get {
                    return kotlin_Char_Companion_MIN_SUPPLEMENTARY_CODE_POINT_get(self.__externalRCRef())
                }
            }
            @_spi(kotlin$experimental$ExperimentalNativeApi)
            public var MIN_CODE_POINT: Swift.Int32 {
                @_spi(kotlin$experimental$ExperimentalNativeApi)
                get {
                    return kotlin_Char_Companion_MIN_CODE_POINT_get(self.__externalRCRef())
                }
            }
            @_spi(kotlin$experimental$ExperimentalNativeApi)
            public var MAX_CODE_POINT: Swift.Int32 {
                @_spi(kotlin$experimental$ExperimentalNativeApi)
                get {
                    return kotlin_Char_Companion_MAX_CODE_POINT_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "Introduce your own constant with the value of `2`. Replacement: 2")
            public var MIN_RADIX: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MIN_RADIX_get(self.__externalRCRef())
                }
            }
            @available(*, deprecated, message: "Introduce your own constant with the value of `36. Replacement: 36")
            public var MAX_RADIX: Swift.Int32 {
                get {
                    return kotlin_Char_Companion_MAX_RADIX_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Char.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Char.Companion.__createClassWrapper(externalRCRef: kotlin_Char_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Int32 {
            return kotlin_Char_compareTo__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Int32 {
            return kotlin_Char_minus__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Char,
            other: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            this._minus(other: other)
        }
        public func inc() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> ExportedKotlinPackages.kotlin.ranges.CharRange {
            return ExportedKotlinPackages.kotlin.ranges.CharRange.__createClassWrapper(externalRCRef: kotlin_Char_rangeTo__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.Unicode.UTF16.CodeUnit
        ) -> ExportedKotlinPackages.kotlin.ranges.CharRange {
            return ExportedKotlinPackages.kotlin.ranges.CharRange.__createClassWrapper(externalRCRef: kotlin_Char_rangeUntil__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), other))
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toByte()")
        public func toByte() -> Swift.Int8 {
            return kotlin_Char_toByte(self.__externalRCRef())
        }
        public func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Char_toChar(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toShort()")
        public func toShort() -> Swift.Int16 {
            return kotlin_Char_toShort(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code")
        public func toInt() -> Swift.Int32 {
            return kotlin_Char_toInt(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toLong()")
        public func toLong() -> Swift.Int64 {
            return kotlin_Char_toLong(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toFloat()")
        public func toFloat() -> Swift.Float {
            return kotlin_Char_toFloat(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Conversion of Char to Number is deprecated. Use Char.code property instead.. Replacement: this.code.toDouble()")
        public func toDouble() -> Swift.Double {
            return kotlin_Char_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Byte: ExportedKotlinPackages.kotlin.Number, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Int8
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Int8 {
                get {
                    return kotlin_Byte_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int8 {
                get {
                    return kotlin_Byte_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Byte_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Byte_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Byte.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Byte.Companion.__createClassWrapper(externalRCRef: kotlin_Byte_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Byte_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Byte_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Byte,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int8 {
            return kotlin_Byte_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int8 {
            return kotlin_Byte_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int32 {
            return kotlin_Byte_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Byte
        ) -> Swift.Int32 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int32 {
            return kotlin_Byte_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Byte
        ) -> Swift.Int32 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Byte_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Byte_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Byte_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Byte_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Byte_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Byte_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Byte_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Byte_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Byte_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Short: ExportedKotlinPackages.kotlin.Number, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Int16
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Int16 {
                get {
                    return kotlin_Short_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int16 {
                get {
                    return kotlin_Short_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Short_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Short_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Short.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Short.Companion.__createClassWrapper(externalRCRef: kotlin_Short_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Short_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Short_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Short_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Short_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Short,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int16 {
            return kotlin_Short_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int16 {
            return kotlin_Short_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int32 {
            return kotlin_Short_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Short
        ) -> Swift.Int32 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int32 {
            return kotlin_Short_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Short
        ) -> Swift.Int32 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Short_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Short_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Short_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Short_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Short_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Short_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Short_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Short_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Short_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Int: ExportedKotlinPackages.kotlin.Number, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Int32
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Int_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Int.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Int.Companion.__createClassWrapper(externalRCRef: kotlin_Int_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Int_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int8
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int16
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int32
        ) -> Swift.Int32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Int_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Int_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Int_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Int,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int32 {
            return kotlin_Int_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int32 {
            return kotlin_Int_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int32 {
            return kotlin_Int_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Int
        ) -> Swift.Int32 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int32 {
            return kotlin_Int_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Int
        ) -> Swift.Int32 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Int_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int32> {
            return { let _ref = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_intRange_getStart_int_KotlinStdlib(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Int_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func ushr(
            bitCount: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_ushr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_and__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_or__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Int_xor__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.Int32 {
            return kotlin_Int_inv(self.__externalRCRef())
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Int_toByte(self.__externalRCRef())
        }
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Int_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Int_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Int_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Int_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Int_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Int_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Long: ExportedKotlinPackages.kotlin.Number, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Int64
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Int64 {
                get {
                    return kotlin_Long_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Int64 {
                get {
                    return kotlin_Long_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Long_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Long_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Long.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Long.Companion.__createClassWrapper(externalRCRef: kotlin_Long_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Long_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int8
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int16
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int32
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Int64
        ) -> Swift.Int64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Long_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Long_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Long,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Int64 {
            return kotlin_Long_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Int64 {
            return kotlin_Long_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Int64 {
            return kotlin_Long_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Long
        ) -> Swift.Int64 {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Int64 {
            return kotlin_Long_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Long
        ) -> Swift.Int64 {
            this._unaryMinus()
        }
        public func rangeTo(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeTo(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int8
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int16
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int32
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func rangeUntil(
            other: Swift.Int64
        ) -> Swift.ClosedRange<Swift.Int64> {
            return { let _ref = kotlin_Long_rangeUntil__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other); return kotlin_ranges_longRange_getStart_long_KotlinStdlib(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_KotlinStdlib(_ref) }()
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func ushr(
            bitCount: Swift.Int32
        ) -> Swift.Int64 {
            return kotlin_Long_ushr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_and__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_or__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.Int64
        ) -> Swift.Int64 {
            return kotlin_Long_xor__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.Int64 {
            return kotlin_Long_inv(self.__externalRCRef())
        }
        public override func toByte() -> Swift.Int8 {
            return kotlin_Long_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Long_toChar(self.__externalRCRef())
        }
        public override func toShort() -> Swift.Int16 {
            return kotlin_Long_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Long_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Long_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Long_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Long_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Float: ExportedKotlinPackages.kotlin.Number, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Float
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Float {
                get {
                    return kotlin_Float_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Float {
                get {
                    return kotlin_Float_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var POSITIVE_INFINITY: Swift.Float {
                get {
                    return kotlin_Float_Companion_POSITIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NEGATIVE_INFINITY: Swift.Float {
                get {
                    return kotlin_Float_Companion_NEGATIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NaN: Swift.Float {
                get {
                    return kotlin_Float_Companion_NaN_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Float_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Float_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Float.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Float.Companion.__createClassWrapper(externalRCRef: kotlin_Float_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Float_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int8
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int16
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int32
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Int64
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Float {
            return kotlin_Float_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Float
        ) -> Swift.Float {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Float_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Float,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Float {
            return kotlin_Float_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Float {
            return kotlin_Float_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Float {
            return kotlin_Float_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Float
        ) -> Swift.Float {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Float {
            return kotlin_Float_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Float
        ) -> Swift.Float {
            this._unaryMinus()
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.. Replacement: toInt().toByte()")
        public override func toByte() -> Swift.Int8 {
            return kotlin_Float_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Float_toChar(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.. Replacement: toInt().toShort()")
        public override func toShort() -> Swift.Int16 {
            return kotlin_Float_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Float_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Float_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Float_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Float_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Double: ExportedKotlinPackages.kotlin.Number, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.Double
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.Double {
                get {
                    return kotlin_Double_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.Double {
                get {
                    return kotlin_Double_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var POSITIVE_INFINITY: Swift.Double {
                get {
                    return kotlin_Double_Companion_POSITIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NEGATIVE_INFINITY: Swift.Double {
                get {
                    return kotlin_Double_Companion_NEGATIVE_INFINITY_get(self.__externalRCRef())
                }
            }
            public var NaN: Swift.Double {
                get {
                    return kotlin_Double_Companion_NaN_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_Double_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_Double_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.Double.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Double.Companion.__createClassWrapper(externalRCRef: kotlin_Double_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.Int8
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int16
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Int64
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Float
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.Double
        ) -> Swift.Int32 {
            return kotlin_Double_compareTo__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_plus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_minus__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _times(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._times(other: other)
        }
        public func _div(
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _div(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.Int8
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int8
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int16
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int16
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int32
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int32
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Int64
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Int64
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Float
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Float__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Float
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.Double
        ) -> Swift.Double {
            return kotlin_Double_rem__TypesOfArguments__Swift_Double__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.Double,
            other: Swift.Double
        ) -> Swift.Double {
            this._rem(other: other)
        }
        public func inc() -> Swift.Double {
            return kotlin_Double_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.Double {
            return kotlin_Double_dec(self.__externalRCRef())
        }
        public func _unaryPlus() -> Swift.Double {
            return kotlin_Double_unaryPlus(self.__externalRCRef())
        }
        public static prefix func +(
            this: ExportedKotlinPackages.kotlin.Double
        ) -> Swift.Double {
            this._unaryPlus()
        }
        public func _unaryMinus() -> Swift.Double {
            return kotlin_Double_unaryMinus(self.__externalRCRef())
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.Double
        ) -> Swift.Double {
            this._unaryMinus()
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Byte.. Replacement: toInt().toByte()")
        public override func toByte() -> Swift.Int8 {
            return kotlin_Double_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.. Replacement: this.toInt().toChar()")
        public override func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Double_toChar(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Unclear conversion. To achieve the same result convert to Int explicitly and then to Short.. Replacement: toInt().toShort()")
        public override func toShort() -> Swift.Int16 {
            return kotlin_Double_toShort(self.__externalRCRef())
        }
        public override func toInt() -> Swift.Int32 {
            return kotlin_Double_toInt(self.__externalRCRef())
        }
        public override func toLong() -> Swift.Int64 {
            return kotlin_Double_toLong(self.__externalRCRef())
        }
        public override func toFloat() -> Swift.Float {
            return kotlin_Double_toFloat(self.__externalRCRef())
        }
        public override func toDouble() -> Swift.Double {
            return kotlin_Double_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class String: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable, ExportedKotlinPackages.kotlin.CharSequence, ExportedKotlinPackages.kotlin.__CharSequence {
        public typealias T = Swift.String
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.String.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.String.Companion.__createClassWrapper(externalRCRef: kotlin_String_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public var length: Swift.Int32 {
            get {
                return kotlin_String_length_get(self.__externalRCRef())
            }
        }
        public func _plus(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.String {
            return kotlin_String_plus__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (other).flatMap { it in it.__retainedToKotlinPointer() })
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.String,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.String {
            this._plus(other: other)
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_String_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_String_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex), conformsTo: ExportedKotlinPackages.kotlin.CharSequence.Type.self) as! any ExportedKotlinPackages.kotlin.CharSequence
        }
        public func _compareTo(
            other: Swift.String
        ) -> Swift.Int32 {
            return kotlin_String_compareTo__TypesOfArguments__Swift_String__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.String,
            other: Swift.String
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public override init() {
            let __kt = kotlin_String_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_String_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            get {
                _get(index: index)
            }
        }
    }
    open class Throwable: KotlinRuntime.KotlinBase, Swift.Error {
        open var message: Swift.String? {
            get {
                if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                    return kotlin_Throwable_message_get(self.__externalRCRef())
                } else {
                    return kotlin_Throwable_message_get_direct(self.__externalRCRef())
                }
            }
        }
        open var cause: ExportedKotlinPackages.kotlin.Throwable? {
            get {
                if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                    return { switch kotlin_Throwable_cause_get(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                } else {
                    return { switch kotlin_Throwable_cause_get_direct(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                }
            }
        }
        @_spi(kotlin$experimental$ExperimentalNativeApi)
        public final func getStackTrace() -> ExportedKotlinPackages.kotlin.Array<Swift.String> {
            return ExportedKotlinPackages.kotlin.Array<Swift.String>.__fromKotlinPointer(kotlin_Throwable_getStackTrace(self.__externalRCRef()))
        }
        public final func printStackTrace() -> Swift.Void {
            return { kotlin_Throwable_printStackTrace(self.__externalRCRef()); return () }()
        }
        public init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Throwable.self {
                 __kt = kotlin_Throwable_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class Exception: ExportedKotlinPackages.kotlin.Throwable {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.Exception.self {
                 __kt = kotlin_Exception_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class RuntimeException: ExportedKotlinPackages.kotlin.Exception {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.RuntimeException.self {
                 __kt = kotlin_RuntimeException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class IllegalArgumentException: ExportedKotlinPackages.kotlin.RuntimeException {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self {
                 __kt = kotlin_IllegalArgumentException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        public override init(
            message: Swift.String?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self {
                 __kt = kotlin_IllegalArgumentException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        public override init(
            message: Swift.String?,
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self {
                 __kt = kotlin_IllegalArgumentException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        public override init(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlin.IllegalArgumentException.self {
                 __kt = kotlin_IllegalArgumentException_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_IllegalArgumentException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Pair<A: KotlinRuntimeSupport._KotlinBridgeable, B: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var first: A {
            get {
                return A.__fromKotlinPointer(_cbridge_kotlin_Pair_first_get(self.__externalRCRef()))
            }
        }
        @inlinable
        public var second: B {
            get {
                return B.__fromKotlinPointer(_cbridge_kotlin_Pair_second_get(self.__externalRCRef()))
            }
        }
        @inlinable
        public func copy(
            first: A,
            second: B
        ) -> ExportedKotlinPackages.kotlin.Pair<A,B> {
            return ExportedKotlinPackages.kotlin.Pair<A,B>.__fromKotlinPointer(_cbridge_kotlin_Pair_copy__TypesOfArguments__A_B__(self.__externalRCRef(), first.__retainedToKotlinPointer(), second.__retainedToKotlinPointer()))
        }
        public init(
            first: A,
            second: B
        ) {
            let __kt = kotlin_Pair_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
            { kotlin_Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_A_B__(__kt, first.__retainedToKotlinPointer(), second.__retainedToKotlinPointer()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Triple<A: KotlinRuntimeSupport._KotlinBridgeable, B: KotlinRuntimeSupport._KotlinBridgeable, C: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var first: A {
            get {
                return A.__fromKotlinPointer(_cbridge_kotlin_Triple_first_get(self.__externalRCRef()))
            }
        }
        @inlinable
        public var second: B {
            get {
                return B.__fromKotlinPointer(_cbridge_kotlin_Triple_second_get(self.__externalRCRef()))
            }
        }
        @inlinable
        public var third: C {
            get {
                return C.__fromKotlinPointer(_cbridge_kotlin_Triple_third_get(self.__externalRCRef()))
            }
        }
        @inlinable
        public func copy(
            first: A,
            second: B,
            third: C
        ) -> ExportedKotlinPackages.kotlin.Triple<A,B,C> {
            return ExportedKotlinPackages.kotlin.Triple<A,B,C>.__fromKotlinPointer(_cbridge_kotlin_Triple_copy__TypesOfArguments__A_B_C__(self.__externalRCRef(), first.__retainedToKotlinPointer(), second.__retainedToKotlinPointer(), third.__retainedToKotlinPointer()))
        }
        public init(
            first: A,
            second: B,
            third: C
        ) {
            let __kt = kotlin_Triple_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
            { kotlin_Triple_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_A_B_C__(__kt, first.__retainedToKotlinPointer(), second.__retainedToKotlinPointer(), third.__retainedToKotlinPointer()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class UByte: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.UInt8
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.UInt8 {
                get {
                    return kotlin_UByte_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.UInt8 {
                get {
                    return kotlin_UByte_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_UByte_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_UByte_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.UByte.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.UByte.Companion.__createClassWrapper(externalRCRef: kotlin_UByte_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_UByte_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UByte,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UByte_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt8 {
            return kotlin_UByte_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt8 {
            return kotlin_UByte_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt8
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UByte_rangeTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt8
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UByte_rangeUntil__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other))
        }
        public func and(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_and__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_or__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UByte_xor__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt8 {
            return kotlin_UByte_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_UByte_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_UByte_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_UByte_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_UByte_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_UByte_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_UByte_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_UByte_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_UByte_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_UByte_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_UByte_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlin$ExperimentalUnsignedTypes)
    public final class UByteArray: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection {
        public typealias E = Swift.UInt8
        public typealias T = Swift.UInt8
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public var size: Swift.Int32 {
            @_spi(kotlin$ExperimentalUnsignedTypes)
            get {
                return kotlin_UByteArray_size_get(self.__externalRCRef())
            }
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _get(
            index: Swift.Int32
        ) -> Swift.UInt8 {
            return kotlin_UByteArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _set(
            index: Swift.Int32,
            value: Swift.UInt8
        ) -> Swift.Void {
            return { kotlin_UByteArray_set__TypesOfArguments__Swift_Int32_Swift_UInt8__(self.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt8> {
            return { let _ptr = kotlin_UByteArray_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt8>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<Swift.UInt8>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func contains(
            element: Swift.UInt8
        ) -> Swift.Bool {
            return kotlin_UByteArray_contains__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.UByteArray,
            element: Swift.UInt8
        ) -> Swift.Bool {
            this.contains(element: element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<Swift.UInt8>
        ) -> Swift.Bool {
            return kotlin_UByteArray_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_Swift_UInt8___(self.__externalRCRef(), elements.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func isEmpty() -> Swift.Bool {
            return kotlin_UByteArray_isEmpty(self.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public subscript(
            index: Swift.Int32
        ) -> Swift.UInt8 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class UInt: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.UInt32
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.UInt32 {
                get {
                    return kotlin_UInt_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.UInt32 {
                get {
                    return kotlin_UInt_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_UInt_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_UInt_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.UInt.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.UInt.Companion.__createClassWrapper(externalRCRef: kotlin_UInt_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_UInt_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UInt,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UInt_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt32 {
            return kotlin_UInt_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt32 {
            return kotlin_UInt_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt32
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UInt_rangeTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt32
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UInt_rangeUntil__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other))
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.UInt32 {
            return kotlin_UInt_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.UInt32 {
            return kotlin_UInt_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_and__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_or__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UInt_xor__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt32 {
            return kotlin_UInt_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_UInt_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_UInt_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_UInt_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_UInt_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_UInt_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_UInt_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_UInt_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_UInt_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_UInt_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_UInt_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlin$ExperimentalUnsignedTypes)
    public final class UIntArray: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection {
        public typealias E = Swift.UInt32
        public typealias T = Swift.UInt32
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public var size: Swift.Int32 {
            @_spi(kotlin$ExperimentalUnsignedTypes)
            get {
                return kotlin_UIntArray_size_get(self.__externalRCRef())
            }
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _get(
            index: Swift.Int32
        ) -> Swift.UInt32 {
            return kotlin_UIntArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _set(
            index: Swift.Int32,
            value: Swift.UInt32
        ) -> Swift.Void {
            return { kotlin_UIntArray_set__TypesOfArguments__Swift_Int32_Swift_UInt32__(self.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt32> {
            return { let _ptr = kotlin_UIntArray_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt32>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<Swift.UInt32>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func contains(
            element: Swift.UInt32
        ) -> Swift.Bool {
            return kotlin_UIntArray_contains__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.UIntArray,
            element: Swift.UInt32
        ) -> Swift.Bool {
            this.contains(element: element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<Swift.UInt32>
        ) -> Swift.Bool {
            return kotlin_UIntArray_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_Swift_UInt32___(self.__externalRCRef(), elements.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func isEmpty() -> Swift.Bool {
            return kotlin_UIntArray_isEmpty(self.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public subscript(
            index: Swift.Int32
        ) -> Swift.UInt32 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class ULong: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.UInt64
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.UInt64 {
                get {
                    return kotlin_ULong_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.UInt64 {
                get {
                    return kotlin_ULong_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_ULong_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_ULong_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ULong.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ULong.Companion.__createClassWrapper(externalRCRef: kotlin_ULong_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_ULong_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.ULong,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt64 {
            return kotlin_ULong_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt64 {
            return kotlin_ULong_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt64
        ) -> ExportedKotlinPackages.kotlin.ranges.ULongRange {
            return ExportedKotlinPackages.kotlin.ranges.ULongRange.__createClassWrapper(externalRCRef: kotlin_ULong_rangeTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt64
        ) -> ExportedKotlinPackages.kotlin.ranges.ULongRange {
            return ExportedKotlinPackages.kotlin.ranges.ULongRange.__createClassWrapper(externalRCRef: kotlin_ULong_rangeUntil__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other))
        }
        public func shl(
            bitCount: Swift.Int32
        ) -> Swift.UInt64 {
            return kotlin_ULong_shl__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func shr(
            bitCount: Swift.Int32
        ) -> Swift.UInt64 {
            return kotlin_ULong_shr__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), bitCount)
        }
        public func and(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_and__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_or__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_ULong_xor__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt64 {
            return kotlin_ULong_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_ULong_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_ULong_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_ULong_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_ULong_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_ULong_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_ULong_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_ULong_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_ULong_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_ULong_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_ULong_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlin$ExperimentalUnsignedTypes)
    public final class ULongArray: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection {
        public typealias E = Swift.UInt64
        public typealias T = Swift.UInt64
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public var size: Swift.Int32 {
            @_spi(kotlin$ExperimentalUnsignedTypes)
            get {
                return kotlin_ULongArray_size_get(self.__externalRCRef())
            }
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _get(
            index: Swift.Int32
        ) -> Swift.UInt64 {
            return kotlin_ULongArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _set(
            index: Swift.Int32,
            value: Swift.UInt64
        ) -> Swift.Void {
            return { kotlin_ULongArray_set__TypesOfArguments__Swift_Int32_Swift_UInt64__(self.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt64> {
            return { let _ptr = kotlin_ULongArray_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt64>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<Swift.UInt64>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func contains(
            element: Swift.UInt64
        ) -> Swift.Bool {
            return kotlin_ULongArray_contains__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ULongArray,
            element: Swift.UInt64
        ) -> Swift.Bool {
            this.contains(element: element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<Swift.UInt64>
        ) -> Swift.Bool {
            return kotlin_ULongArray_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_Swift_UInt64___(self.__externalRCRef(), elements.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func isEmpty() -> Swift.Bool {
            return kotlin_ULongArray_isEmpty(self.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public subscript(
            index: Swift.Int32
        ) -> Swift.UInt64 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class UShort: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = Swift.UInt16
        public final class Companion: KotlinRuntime.KotlinBase {
            public var MIN_VALUE: Swift.UInt16 {
                get {
                    return kotlin_UShort_Companion_MIN_VALUE_get(self.__externalRCRef())
                }
            }
            public var MAX_VALUE: Swift.UInt16 {
                get {
                    return kotlin_UShort_Companion_MAX_VALUE_get(self.__externalRCRef())
                }
            }
            public var SIZE_BYTES: Swift.Int32 {
                get {
                    return kotlin_UShort_Companion_SIZE_BYTES_get(self.__externalRCRef())
                }
            }
            public var SIZE_BITS: Swift.Int32 {
                get {
                    return kotlin_UShort_Companion_SIZE_BITS_get(self.__externalRCRef())
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.UShort.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.UShort.Companion.__createClassWrapper(externalRCRef: kotlin_UShort_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public func _compareTo(
            other: Swift.UInt8
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt16
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt32
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: Swift.UInt64
        ) -> Swift.Int32 {
            return kotlin_UShort_compareTo__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _plus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_plus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._plus(other: other)
        }
        public func _minus(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._minus(other: other)
        }
        public func _minus(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_minus__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._minus(other: other)
        }
        public func _times(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._times(other: other)
        }
        public func _times(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_times__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._times(other: other)
        }
        public func _div(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._div(other: other)
        }
        public func _div(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_div__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._div(other: other)
        }
        public func _rem(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            this._rem(other: other)
        }
        public func _rem(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_rem__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public static func %(
            this: ExportedKotlinPackages.kotlin.UShort,
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            this._rem(other: other)
        }
        public func floorDiv(
            other: Swift.UInt8
        ) -> Swift.UInt32 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt16
        ) -> Swift.UInt32 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func floorDiv(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_floorDiv__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt8
        ) -> Swift.UInt8 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt8__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt32
        ) -> Swift.UInt32 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), other)
        }
        public func mod(
            other: Swift.UInt64
        ) -> Swift.UInt64 {
            return kotlin_UShort_mod__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), other)
        }
        public func inc() -> Swift.UInt16 {
            return kotlin_UShort_inc(self.__externalRCRef())
        }
        public func dec() -> Swift.UInt16 {
            return kotlin_UShort_dec(self.__externalRCRef())
        }
        public func rangeTo(
            other: Swift.UInt16
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UShort_rangeTo__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other))
        }
        public func rangeUntil(
            other: Swift.UInt16
        ) -> ExportedKotlinPackages.kotlin.ranges.UIntRange {
            return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_UShort_rangeUntil__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other))
        }
        public func and(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_and__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func or(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_or__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func xor(
            other: Swift.UInt16
        ) -> Swift.UInt16 {
            return kotlin_UShort_xor__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), other)
        }
        public func inv() -> Swift.UInt16 {
            return kotlin_UShort_inv(self.__externalRCRef())
        }
        public func toByte() -> Swift.Int8 {
            return kotlin_UShort_toByte(self.__externalRCRef())
        }
        public func toShort() -> Swift.Int16 {
            return kotlin_UShort_toShort(self.__externalRCRef())
        }
        public func toInt() -> Swift.Int32 {
            return kotlin_UShort_toInt(self.__externalRCRef())
        }
        public func toLong() -> Swift.Int64 {
            return kotlin_UShort_toLong(self.__externalRCRef())
        }
        public func toUByte() -> Swift.UInt8 {
            return kotlin_UShort_toUByte(self.__externalRCRef())
        }
        public func toUShort() -> Swift.UInt16 {
            return kotlin_UShort_toUShort(self.__externalRCRef())
        }
        public func toUInt() -> Swift.UInt32 {
            return kotlin_UShort_toUInt(self.__externalRCRef())
        }
        public func toULong() -> Swift.UInt64 {
            return kotlin_UShort_toULong(self.__externalRCRef())
        }
        public func toFloat() -> Swift.Float {
            return kotlin_UShort_toFloat(self.__externalRCRef())
        }
        public func toDouble() -> Swift.Double {
            return kotlin_UShort_toDouble(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlin$ExperimentalUnsignedTypes)
    public final class UShortArray: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection {
        public typealias E = Swift.UInt16
        public typealias T = Swift.UInt16
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public var size: Swift.Int32 {
            @_spi(kotlin$ExperimentalUnsignedTypes)
            get {
                return kotlin_UShortArray_size_get(self.__externalRCRef())
            }
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _get(
            index: Swift.Int32
        ) -> Swift.UInt16 {
            return kotlin_UShortArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func _set(
            index: Swift.Int32,
            value: Swift.UInt16
        ) -> Swift.Void {
            return { kotlin_UShortArray_set__TypesOfArguments__Swift_Int32_Swift_UInt16__(self.__externalRCRef(), index, value); return () }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt16> {
            return { let _ptr = kotlin_UShortArray_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt16>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<Swift.UInt16>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func contains(
            element: Swift.UInt16
        ) -> Swift.Bool {
            return kotlin_UShortArray_contains__TypesOfArguments__Swift_UInt16__(self.__externalRCRef(), element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.UShortArray,
            element: Swift.UInt16
        ) -> Swift.Bool {
            this.contains(element: element)
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<Swift.UInt16>
        ) -> Swift.Bool {
            return kotlin_UShortArray_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_Swift_UInt16___(self.__externalRCRef(), elements.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public func isEmpty() -> Swift.Bool {
            return kotlin_UShortArray_isEmpty(self.__externalRCRef())
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlin$ExperimentalUnsignedTypes)
        public subscript(
            index: Swift.Int32
        ) -> Swift.UInt16 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class AnyComparable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    open class Number: KotlinRuntime.KotlinBase {
        open func toDouble() -> Swift.Double {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toDouble(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toDouble': a Swift subclass must override it and must not call super.")
            }
        }
        open func toFloat() -> Swift.Float {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toFloat(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toFloat': a Swift subclass must override it and must not call super.")
            }
        }
        open func toLong() -> Swift.Int64 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toLong(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toLong': a Swift subclass must override it and must not call super.")
            }
        }
        open func toInt() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toInt(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toInt': a Swift subclass must override it and must not call super.")
            }
        }
        @available(*, deprecated, message: """
Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.
If you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.
See https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration. Replacement: this.toInt().toChar()
""")
        open func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toChar(self.__externalRCRef())
            } else {
                return kotlin_Number_toChar_direct(self.__externalRCRef())
            }
        }
        open func toShort() -> Swift.Int16 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toShort(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toShort': a Swift subclass must override it and must not call super.")
            }
        }
        open func toByte() -> Swift.Int8 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toByte(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toByte': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.Number.self, "ExportedKotlinPackages.kotlin.Number is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Number_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Collection<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._Collection where Self.T == E {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
        var size: Swift.Int32 {
            get
        }
        func isEmpty() -> Swift.Bool
        func contains(
            element: E
        ) -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Collection)
    public protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    public protocol __Collection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol Iterable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterable {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T>
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterable)
    public protocol _Iterable {
    }
    public protocol __Iterable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Map<K, V>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Map {
        associatedtype K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable
        associatedtype V: KotlinRuntimeSupport._KotlinBridgeable
        var size: Swift.Int32 {
            get
        }
        var keys: Swift.Set<K> {
            get
        }
        var values: any ExportedKotlinPackages.kotlin.collections.Collection<V> {
            get
        }
        var entries: Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>> {
            get
        }
        func isEmpty() -> Swift.Bool
        func containsKey(
            key: K
        ) -> Swift.Bool
        func containsValue(
            value: V
        ) -> Swift.Bool
        func _get(
            key: K
        ) -> V?
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Map)
    public protocol _Map {
    }
    public protocol __Map: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Iterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterator {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func next() -> T
        func hasNext() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterator)
    public protocol _Iterator {
    }
    public protocol __Iterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class AnyCollection<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyIterable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyMap<K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Map, ExportedKotlinPackages.kotlin.collections.__Map, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    open class ByteIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int8
        public final func next() -> Swift.Int8 {
            return kotlin_collections_ByteIterator_next(self.__externalRCRef())
        }
        open func nextByte() -> Swift.Int8 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.ByteIterator.self {
                return kotlin_collections_ByteIterator_nextByte(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.ByteIterator.nextByte': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.ByteIterator.self, "ExportedKotlinPackages.kotlin.collections.ByteIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_ByteIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class CharIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Unicode.UTF16.CodeUnit
        public final func next() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_collections_CharIterator_next(self.__externalRCRef())
        }
        open func nextChar() -> Swift.Unicode.UTF16.CodeUnit {
            if Self.self == ExportedKotlinPackages.kotlin.collections.CharIterator.self {
                return kotlin_collections_CharIterator_nextChar(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.CharIterator.nextChar': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.CharIterator.self, "ExportedKotlinPackages.kotlin.collections.CharIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_CharIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class ShortIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int16
        public final func next() -> Swift.Int16 {
            return kotlin_collections_ShortIterator_next(self.__externalRCRef())
        }
        open func nextShort() -> Swift.Int16 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.ShortIterator.self {
                return kotlin_collections_ShortIterator_nextShort(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.ShortIterator.nextShort': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.ShortIterator.self, "ExportedKotlinPackages.kotlin.collections.ShortIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_ShortIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class IntIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int32
        public final func next() -> Swift.Int32 {
            return kotlin_collections_IntIterator_next(self.__externalRCRef())
        }
        open func nextInt() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.IntIterator.self {
                return kotlin_collections_IntIterator_nextInt(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.IntIterator.nextInt': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.IntIterator.self, "ExportedKotlinPackages.kotlin.collections.IntIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_IntIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class LongIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int64
        public final func next() -> Swift.Int64 {
            return kotlin_collections_LongIterator_next(self.__externalRCRef())
        }
        open func nextLong() -> Swift.Int64 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.LongIterator.self {
                return kotlin_collections_LongIterator_nextLong(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.LongIterator.nextLong': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.LongIterator.self, "ExportedKotlinPackages.kotlin.collections.LongIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_LongIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class FloatIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Float
        public final func next() -> Swift.Float {
            return kotlin_collections_FloatIterator_next(self.__externalRCRef())
        }
        open func nextFloat() -> Swift.Float {
            if Self.self == ExportedKotlinPackages.kotlin.collections.FloatIterator.self {
                return kotlin_collections_FloatIterator_nextFloat(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.FloatIterator.nextFloat': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.FloatIterator.self, "ExportedKotlinPackages.kotlin.collections.FloatIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_FloatIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class DoubleIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Double
        public final func next() -> Swift.Double {
            return kotlin_collections_DoubleIterator_next(self.__externalRCRef())
        }
        open func nextDouble() -> Swift.Double {
            if Self.self == ExportedKotlinPackages.kotlin.collections.DoubleIterator.self {
                return kotlin_collections_DoubleIterator_nextDouble(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.DoubleIterator.nextDouble': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.DoubleIterator.self, "ExportedKotlinPackages.kotlin.collections.DoubleIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_DoubleIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class BooleanIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Bool
        public final func next() -> Swift.Bool {
            return kotlin_collections_BooleanIterator_next(self.__externalRCRef())
        }
        open func nextBoolean() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlin.collections.BooleanIterator.self {
                return kotlin_collections_BooleanIterator_nextBoolean(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.BooleanIterator.nextBoolean': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.BooleanIterator.self, "ExportedKotlinPackages.kotlin.collections.BooleanIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_BooleanIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin.time {
    public enum DurationUnit: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case NANOSECONDS
        case MICROSECONDS
        case MILLISECONDS
        case SECONDS
        case MINUTES
        case HOURS
        case DAYS
        public var description: Swift.String {
            get {
                switch self {
                case .NANOSECONDS: "NANOSECONDS"
                case .MICROSECONDS: "MICROSECONDS"
                case .MILLISECONDS: "MILLISECONDS"
                case .SECONDS: "SECONDS"
                case .MINUTES: "MINUTES"
                case .HOURS: "HOURS"
                case .DAYS: "DAYS"
                default: fatalError()
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .NANOSECONDS: 0
                case .MICROSECONDS: 1
                case .MILLISECONDS: 2
                case .SECONDS: 3
                case .MINUTES: 4
                case .HOURS: 5
                case .DAYS: 6
                default: fatalError()
                }
            }
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlin_time_DurationUnit_ordinal(__externalRCRefUnsafe) {
            case 0: self = .NANOSECONDS
            case 1: self = .MICROSECONDS
            case 2: self = .MILLISECONDS
            case 3: self = .SECONDS
            case 4: self = .MINUTES
            case 5: self = .HOURS
            case 6: self = .DAYS
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
            return switch self {
            case .NANOSECONDS: kotlin_time_DurationUnit_NANOSECONDS()
            case .MICROSECONDS: kotlin_time_DurationUnit_MICROSECONDS()
            case .MILLISECONDS: kotlin_time_DurationUnit_MILLISECONDS()
            case .SECONDS: kotlin_time_DurationUnit_SECONDS()
            case .MINUTES: kotlin_time_DurationUnit_MINUTES()
            case .HOURS: kotlin_time_DurationUnit_HOURS()
            case .DAYS: kotlin_time_DurationUnit_DAYS()
            default: fatalError()
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "NANOSECONDS": self = .NANOSECONDS
            case "MICROSECONDS": self = .MICROSECONDS
            case "MILLISECONDS": self = .MILLISECONDS
            case "SECONDS": self = .SECONDS
            case "MINUTES": self = .MINUTES
            case "HOURS": self = .HOURS
            case "DAYS": self = .DAYS
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<7 ~= rawValue else { return nil }
            self = DurationUnit.allCases[Int(rawValue)]
        }
    }
    public final class Duration: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias T = ExportedKotlinPackages.kotlin.time.Duration
        public final class Companion: KotlinRuntime.KotlinBase {
            public var ZERO: ExportedKotlinPackages.kotlin.time.Duration {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_ZERO_get(self.__externalRCRef()))
                }
            }
            public var INFINITE: ExportedKotlinPackages.kotlin.time.Duration {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_INFINITE_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.time.Duration.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.time.Duration.Companion.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_get())
                }
            }
            @_spi(kotlin$time$ExperimentalTime)
            public func convert(
                value: Swift.Double,
                sourceUnit: ExportedKotlinPackages.kotlin.time.DurationUnit,
                targetUnit: ExportedKotlinPackages.kotlin.time.DurationUnit
            ) -> Swift.Double {
                return kotlin_time_Duration_Companion_convert__TypesOfArguments__Swift_Double_ExportedKotlinPackages_kotlin_time_DurationUnit_ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), value, sourceUnit.__externalRCRef(), targetUnit.__externalRCRef())
            }
            public func parse(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_parse__TypesOfArguments__Swift_String__(self.__externalRCRef(), value))
            }
            public func parseIsoString(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_parseIsoString__TypesOfArguments__Swift_String__(self.__externalRCRef(), value))
            }
            public func parseOrNull(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration? {
                return { switch kotlin_time_Duration_Companion_parseOrNull__TypesOfArguments__Swift_String__(self.__externalRCRef(), value) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: res); } }()
            }
            public func parseIsoStringOrNull(
                value: Swift.String
            ) -> ExportedKotlinPackages.kotlin.time.Duration? {
                return { switch kotlin_time_Duration_Companion_parseIsoStringOrNull__TypesOfArguments__Swift_String__(self.__externalRCRef(), value) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: res); } }()
            }
            public func getNanoseconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getNanoseconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getNanoseconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getMicroseconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getMicroseconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getMicroseconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getMilliseconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getMilliseconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getMilliseconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getSeconds(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getSeconds(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getSeconds(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getMinutes(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getMinutes(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getMinutes(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getHours(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getHours(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getHours(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            public func getDays(
                _ receiver: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int32__(self.__externalRCRef(), receiver))
            }
            public func getDays(
                _ receiver: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int64__(self.__externalRCRef(), receiver))
            }
            public func getDays(
                _ receiver: Swift.Double
            ) -> ExportedKotlinPackages.kotlin.time.Duration {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Double__(self.__externalRCRef(), receiver))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public var absoluteValue: ExportedKotlinPackages.kotlin.time.Duration {
            get {
                return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_absoluteValue_get(self.__externalRCRef()))
            }
        }
        public var inWholeDays: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeDays_get(self.__externalRCRef())
            }
        }
        public var inWholeHours: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeHours_get(self.__externalRCRef())
            }
        }
        public var inWholeMinutes: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMinutes_get(self.__externalRCRef())
            }
        }
        public var inWholeSeconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeSeconds_get(self.__externalRCRef())
            }
        }
        public var inWholeMilliseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMilliseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeMicroseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeMicroseconds_get(self.__externalRCRef())
            }
        }
        public var inWholeNanoseconds: Swift.Int64 {
            get {
                return kotlin_time_Duration_inWholeNanoseconds_get(self.__externalRCRef())
            }
        }
        public func _unaryMinus() -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_unaryMinus(self.__externalRCRef()))
        }
        public static prefix func -(
            this: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._unaryMinus()
        }
        public func _plus(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_plus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public static func +(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._plus(other: other)
        }
        public func _minus(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_minus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public static func -(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._minus(other: other)
        }
        public func _times(
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scale))
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._times(scale: scale)
        }
        public func _times(
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_times__TypesOfArguments__Swift_Double__(self.__externalRCRef(), scale))
        }
        public static func *(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._times(scale: scale)
        }
        public func _div(
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scale))
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._div(scale: scale)
        }
        public func _div(
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            return ExportedKotlinPackages.kotlin.time.Duration.__createClassWrapper(externalRCRef: kotlin_time_Duration_div__TypesOfArguments__Swift_Double__(self.__externalRCRef(), scale))
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            scale: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.time.Duration {
            this._div(scale: scale)
        }
        public func _div(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Double {
            return kotlin_time_Duration_div__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef())
        }
        public static func /(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Double {
            this._div(other: other)
        }
        public func isNegative() -> Swift.Bool {
            return kotlin_time_Duration_isNegative(self.__externalRCRef())
        }
        public func isPositive() -> Swift.Bool {
            return kotlin_time_Duration_isPositive(self.__externalRCRef())
        }
        public func isInfinite() -> Swift.Bool {
            return kotlin_time_Duration_isInfinite(self.__externalRCRef())
        }
        public func isFinite() -> Swift.Bool {
            return kotlin_time_Duration_isFinite(self.__externalRCRef())
        }
        public func _compareTo(
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Int32 {
            return kotlin_time_Duration_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self.__externalRCRef(), other.__externalRCRef())
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.time.Duration,
            other: ExportedKotlinPackages.kotlin.time.Duration
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32, _p2: Swift.Int32, _p3: Swift.Int32, _p4: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1, _p2, _p3, _p4); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32, _p2: Swift.Int32, _p3: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1, _p2, _p3); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32, _p2: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1, _p2); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toComponents<T: KotlinRuntimeSupport._KotlinBridgeable>(
            action: @escaping (Swift.Int64, Swift.Int32) -> T
        ) -> T {
            return T.__fromKotlinPointer(kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32U29202D_U20T__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int64, _p1: Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (action)(_p0, _p1); return _res.__retainedToKotlinPointer() } as (Swift.Int64, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
        }
        public func toDouble(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Double {
            return kotlin_time_Duration_toDouble__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toLong(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Int64 {
            return kotlin_time_Duration_toLong__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toInt(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit
        ) -> Swift.Int32 {
            return kotlin_time_Duration_toInt__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self.__externalRCRef(), unit.__externalRCRef())
        }
        public func toString(
            unit: ExportedKotlinPackages.kotlin.time.DurationUnit,
            decimals: Swift.Int32
        ) -> Swift.String {
            return kotlin_time_Duration_toString__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit_Swift_Int32__(self.__externalRCRef(), unit.__externalRCRef(), decimals)
        }
        public func toIsoString() -> Swift.String {
            return kotlin_time_Duration_toIsoString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.Annotation where Self : ExportedKotlinPackages.kotlin.__Annotation {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin._Annotation {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Annotation, ExportedKotlinPackages.kotlin.__Annotation where Wrapped : ExportedKotlinPackages.kotlin._Annotation {
}
extension ExportedKotlinPackages.kotlin.Annotation {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Collection where Self : ExportedKotlinPackages.kotlin.collections.__Collection {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_Collection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
extension ExportedKotlinPackages.kotlin.collections.Collection {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : ExportedKotlinPackages.kotlin.collections.__Iterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
        return { let _ptr = kotlin_collections_Iterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Map where Self : ExportedKotlinPackages.kotlin.collections.__Map {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Map_size_get(self.__externalRCRef())
        }
    }
    public var keys: Swift.Set<K> {
        get {
            return Swift.Set<K>.__fromKotlinPointer(kotlin_collections_Map_keys_get(self.__externalRCRef()))
        }
    }
    public var values: any ExportedKotlinPackages.kotlin.collections.Collection<V> {
        get {
            return { let _ptr = kotlin_collections_Map_values_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<V>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<V>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
    }
    public var entries: Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>> {
        get {
            return Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>>.__fromKotlinPointer(kotlin_collections_Map_entries_get(self.__externalRCRef()))
        }
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Map_isEmpty(self.__externalRCRef())
    }
    public func containsKey(
        key: K
    ) -> Swift.Bool {
        return kotlin_collections_Map_containsKey__TypesOfArguments__K_Swift_Hashable__(self.__externalRCRef(), key.__retainedToKotlinPointer())
    }
    public func containsValue(
        value: V
    ) -> Swift.Bool {
        return kotlin_collections_Map_containsValue__TypesOfArguments__V__(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public func _get(
        key: K
    ) -> V? {
        return Swift.Optional<V>.__fromKotlinPointer(kotlin_collections_Map_get__TypesOfArguments__K_Swift_Hashable__(self.__externalRCRef(), key.__retainedToKotlinPointer()))
    }
    public subscript(
        key: K
    ) -> V? {
        get {
            _get(key: key)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Map {
}
extension ExportedKotlinPackages.kotlin.collections.Map {
    public typealias Entry<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable> = KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry<K,V>
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_Map_Entry {
    public var key: K {
        get {
            return K.__fromKotlinPointer(kotlin_collections_Map_Entry_key_get(self.__externalRCRef()))
        }
    }
    public var value: V {
        get {
            return V.__fromKotlinPointer(kotlin_collections_Map_Entry_value_get(self.__externalRCRef()))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
extension ExportedKotlinPackages.kotlin.ranges {
    public protocol ClosedRange<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges._ClosedRange {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        var start: T {
            get
        }
        var endInclusive: T {
            get
        }
        func contains(
            value: T
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_ranges_ClosedRange)
    public protocol _ClosedRange {
    }
    public protocol __ClosedRange: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol OpenEndRange<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges._OpenEndRange {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        var start: T {
            get
        }
        var endExclusive: T {
            get
        }
        func contains(
            value: T
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_ranges_OpenEndRange)
    public protocol _OpenEndRange {
    }
    public protocol __OpenEndRange: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class CharRange: ExportedKotlinPackages.kotlin.ranges.CharProgression, ExportedKotlinPackages.kotlin.ranges.ClosedRange, ExportedKotlinPackages.kotlin.ranges.__ClosedRange, ExportedKotlinPackages.kotlin.ranges.OpenEndRange, ExportedKotlinPackages.kotlin.ranges.__OpenEndRange {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.CharRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.CharRange.__createClassWrapper(externalRCRef: kotlin_ranges_CharRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.CharRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.CharRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_CharRange_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public var start: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with Char type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            return kotlin_ranges_CharRange_contains__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.CharRange,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_CharRange_isEmpty(self.__externalRCRef())
        }
        public init(
            start: Swift.Unicode.UTF16.CodeUnit,
            endInclusive: Swift.Unicode.UTF16.CodeUnit
        ) {
            let __kt = kotlin_ranges_CharRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_ranges_CharRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Unicode_UTF16_CodeUnit_Swift_Unicode_UTF16_CodeUnit__(__kt, start, endInclusive); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class CharProgression: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable {
        public typealias T = Swift.Unicode.UTF16.CodeUnit
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.CharProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.CharProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.Unicode.UTF16.CodeUnit,
                rangeEnd: Swift.Unicode.UTF16.CodeUnit,
                step: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.ranges.CharProgression {
                return ExportedKotlinPackages.kotlin.ranges.CharProgression.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_Companion_fromClosedRange__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit_Swift_Unicode_UTF16_CodeUnit_Swift_Int32__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public final var first: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.Unicode.UTF16.CodeUnit {
            get {
                return kotlin_ranges_CharProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int32 {
            get {
                return kotlin_ranges_CharProgression_step_get(self.__externalRCRef())
            }
        }
        open func iterator() -> ExportedKotlinPackages.kotlin.collections.CharIterator {
            if Self.self == ExportedKotlinPackages.kotlin.ranges.CharProgression.self {
                return ExportedKotlinPackages.kotlin.collections.CharIterator.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_iterator(self.__externalRCRef()))
            } else {
                return ExportedKotlinPackages.kotlin.collections.CharIterator.__createClassWrapper(externalRCRef: kotlin_ranges_CharProgression_iterator_direct(self.__externalRCRef()))
            }
        }
        open func isEmpty() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlin.ranges.CharProgression.self {
                return kotlin_ranges_CharProgression_isEmpty(self.__externalRCRef())
            } else {
                return kotlin_ranges_CharProgression_isEmpty_direct(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class UIntRange: ExportedKotlinPackages.kotlin.ranges.UIntProgression, ExportedKotlinPackages.kotlin.ranges.ClosedRange, ExportedKotlinPackages.kotlin.ranges.__ClosedRange, ExportedKotlinPackages.kotlin.ranges.OpenEndRange, ExportedKotlinPackages.kotlin.ranges.__OpenEndRange {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.UIntRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.UIntRange.__createClassWrapper(externalRCRef: kotlin_ranges_UIntRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.UIntRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.UIntRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_UIntRange_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public var start: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with UInt type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.UInt32
        ) -> Swift.Bool {
            return kotlin_ranges_UIntRange_contains__TypesOfArguments__Swift_UInt32__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.UIntRange,
            value: Swift.UInt32
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_UIntRange_isEmpty(self.__externalRCRef())
        }
        public init(
            start: Swift.UInt32,
            endInclusive: Swift.UInt32
        ) {
            let __kt = kotlin_ranges_UIntRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_ranges_UIntRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__(__kt, start, endInclusive); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class UIntProgression: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable {
        public typealias T = Swift.UInt32
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.UIntProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.UIntProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_UIntProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.UInt32,
                rangeEnd: Swift.UInt32,
                step: Swift.Int32
            ) -> ExportedKotlinPackages.kotlin.ranges.UIntProgression {
                return ExportedKotlinPackages.kotlin.ranges.UIntProgression.__createClassWrapper(externalRCRef: kotlin_ranges_UIntProgression_Companion_fromClosedRange__TypesOfArguments__Swift_UInt32_Swift_UInt32_Swift_Int32__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public final var first: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.UInt32 {
            get {
                return kotlin_ranges_UIntProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int32 {
            get {
                return kotlin_ranges_UIntProgression_step_get(self.__externalRCRef())
            }
        }
        public final func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt32> {
            return { let _ptr = kotlin_ranges_UIntProgression_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt32>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<Swift.UInt32>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        open func isEmpty() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlin.ranges.UIntProgression.self {
                return kotlin_ranges_UIntProgression_isEmpty(self.__externalRCRef())
            } else {
                return kotlin_ranges_UIntProgression_isEmpty_direct(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class ULongRange: ExportedKotlinPackages.kotlin.ranges.ULongProgression, ExportedKotlinPackages.kotlin.ranges.ClosedRange, ExportedKotlinPackages.kotlin.ranges.__ClosedRange, ExportedKotlinPackages.kotlin.ranges.OpenEndRange, ExportedKotlinPackages.kotlin.ranges.__OpenEndRange {
        public final class Companion: KotlinRuntime.KotlinBase {
            public var EMPTY: ExportedKotlinPackages.kotlin.ranges.ULongRange {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.ULongRange.__createClassWrapper(externalRCRef: kotlin_ranges_ULongRange_Companion_EMPTY_get(self.__externalRCRef()))
                }
            }
            public static var shared: ExportedKotlinPackages.kotlin.ranges.ULongRange.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.ULongRange.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_ULongRange_Companion_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public var start: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongRange_start_get(self.__externalRCRef())
            }
        }
        public var endInclusive: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongRange_endInclusive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "Can throw an exception when it's impossible to represent the value with ULong type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
        public var endExclusive: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongRange_endExclusive_get(self.__externalRCRef())
            }
        }
        public func contains(
            value: Swift.UInt64
        ) -> Swift.Bool {
            return kotlin_ranges_ULongRange_contains__TypesOfArguments__Swift_UInt64__(self.__externalRCRef(), value)
        }
        public static func ~=(
            this: ExportedKotlinPackages.kotlin.ranges.ULongRange,
            value: Swift.UInt64
        ) -> Swift.Bool {
            this.contains(value: value)
        }
        public override func isEmpty() -> Swift.Bool {
            return kotlin_ranges_ULongRange_isEmpty(self.__externalRCRef())
        }
        public init(
            start: Swift.UInt64,
            endInclusive: Swift.UInt64
        ) {
            let __kt = kotlin_ranges_ULongRange_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_ranges_ULongRange_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt64_Swift_UInt64__(__kt, start, endInclusive); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class ULongProgression: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable {
        public typealias T = Swift.UInt64
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlin.ranges.ULongProgression.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.ranges.ULongProgression.Companion.__createClassWrapper(externalRCRef: kotlin_ranges_ULongProgression_Companion_get())
                }
            }
            public func fromClosedRange(
                rangeStart: Swift.UInt64,
                rangeEnd: Swift.UInt64,
                step: Swift.Int64
            ) -> ExportedKotlinPackages.kotlin.ranges.ULongProgression {
                return ExportedKotlinPackages.kotlin.ranges.ULongProgression.__createClassWrapper(externalRCRef: kotlin_ranges_ULongProgression_Companion_fromClosedRange__TypesOfArguments__Swift_UInt64_Swift_UInt64_Swift_Int64__(self.__externalRCRef(), rangeStart, rangeEnd, step))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
                fatalError()
            }
        }
        public final var first: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongProgression_first_get(self.__externalRCRef())
            }
        }
        public final var last: Swift.UInt64 {
            get {
                return kotlin_ranges_ULongProgression_last_get(self.__externalRCRef())
            }
        }
        public final var step: Swift.Int64 {
            get {
                return kotlin_ranges_ULongProgression_step_get(self.__externalRCRef())
            }
        }
        public final func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt64> {
            return { let _ptr = kotlin_ranges_ULongProgression_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<Swift.UInt64>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<Swift.UInt64>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        open func isEmpty() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlin.ranges.ULongProgression.self {
                return kotlin_ranges_ULongProgression_isEmpty(self.__externalRCRef())
            } else {
                return kotlin_ranges_ULongProgression_isEmpty_direct(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyClosedRange<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges.ClosedRange, ExportedKotlinPackages.kotlin.ranges.__ClosedRange, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyOpenEndRange<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges.OpenEndRange, ExportedKotlinPackages.kotlin.ranges.__OpenEndRange, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.CharSequence where Self : ExportedKotlinPackages.kotlin.__CharSequence {
    public var length: Swift.Int32 {
        get {
            return kotlin_CharSequence_length_get(self.__externalRCRef())
        }
    }
    public func _get(
        index: Swift.Int32
    ) -> Swift.Unicode.UTF16.CodeUnit {
        return kotlin_CharSequence_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
    public func subSequence(
        startIndex: Swift.Int32,
        endIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.CharSequence {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex), conformsTo: ExportedKotlinPackages.kotlin.CharSequence.Type.self) as! any ExportedKotlinPackages.kotlin.CharSequence
    }
    public subscript(
        index: Swift.Int32
    ) -> Swift.Unicode.UTF16.CodeUnit {
        get {
            _get(index: index)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin._CharSequence {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.CharSequence, ExportedKotlinPackages.kotlin.__CharSequence where Wrapped : ExportedKotlinPackages.kotlin._CharSequence {
}
extension ExportedKotlinPackages.kotlin.CharSequence {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.Comparable where Self : ExportedKotlinPackages.kotlin.__Comparable {
    public func _compareTo(
        other: T
    ) -> Swift.Int32 {
        return kotlin_Comparable_compareTo__TypesOfArguments__T__(self.__externalRCRef(), other.__retainedToKotlinPointer())
    }
    public static func <(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin._Comparable {
}
extension ExportedKotlinPackages.kotlin.Comparable {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : ExportedKotlinPackages.kotlin.collections.__Iterator {
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_Iterator_next(self.__externalRCRef()))
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.ranges.ClosedRange where Self : ExportedKotlinPackages.kotlin.ranges.__ClosedRange {
    public var start: T {
        get {
            return T.__fromKotlinPointer(kotlin_ranges_ClosedRange_start_get(self.__externalRCRef()))
        }
    }
    public var endInclusive: T {
        get {
            return T.__fromKotlinPointer(kotlin_ranges_ClosedRange_endInclusive_get(self.__externalRCRef()))
        }
    }
    public func contains(
        value: T
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public static func ~=(
        this: Self,
        value: T
    ) -> Swift.Bool {
        this.contains(value: value)
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.ranges._ClosedRange {
}
extension ExportedKotlinPackages.kotlin.ranges.ClosedRange {
    public func contains(
        value: T
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__T___direct(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty_direct(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.ranges.OpenEndRange where Self : ExportedKotlinPackages.kotlin.ranges.__OpenEndRange {
    public var start: T {
        get {
            return T.__fromKotlinPointer(kotlin_ranges_OpenEndRange_start_get(self.__externalRCRef()))
        }
    }
    public var endExclusive: T {
        get {
            return T.__fromKotlinPointer(kotlin_ranges_OpenEndRange_endExclusive_get(self.__externalRCRef()))
        }
    }
    public func contains(
        value: T
    ) -> Swift.Bool {
        return kotlin_ranges_OpenEndRange_contains__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public static func ~=(
        this: Self,
        value: T
    ) -> Swift.Bool {
        this.contains(value: value)
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_OpenEndRange_isEmpty(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.ranges._OpenEndRange {
}
extension ExportedKotlinPackages.kotlin.ranges.OpenEndRange {
    public func contains(
        value: T
    ) -> Swift.Bool {
        return kotlin_ranges_OpenEndRange_contains__TypesOfArguments__T___direct(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_OpenEndRange_isEmpty_direct(self.__externalRCRef())
    }
}
@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32, _ _3: Swift.Int32, _ _4: Swift.Int32, _ _5: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3, _4, _5)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32, _ _3: Swift.Int32, _ _4: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3, _4)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32, _ _3: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3)
    return _result
}

@_cdecl("KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32__")
package func KotlinStdlib_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int64_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int64, _ _2: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int64, Swift.Int32) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2)
    return _result
}

@_cdecl("kotlin_CharSequence_get__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_CharSequence_get__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UInt16 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.CharSequence.Type.self) as! any ExportedKotlinPackages.kotlin.CharSequence
    let _result: Swift.Unicode.UTF16.CodeUnit = _self._get(index: index)
    return _result
}

@_cdecl("kotlin_CharSequence_length_get__reverse_swift")
package func kotlin_CharSequence_length_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.CharSequence.Type.self) as! any ExportedKotlinPackages.kotlin.CharSequence
    let _result: Swift.Int32 = _self.length
    return _result
}

@_cdecl("kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ startIndex: Swift.Int32, _ endIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.CharSequence.Type.self) as! any ExportedKotlinPackages.kotlin.CharSequence
    let _result: any ExportedKotlinPackages.kotlin.CharSequence = _self.subSequence(startIndex: startIndex, endIndex: endIndex)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_Comparable_compareTo__TypesOfArguments__T____reverse_swift")
package func kotlin_Comparable_compareTo__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.Comparable)
    func _open<__Self: ExportedKotlinPackages.kotlin.Comparable>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self._compareTo(other: __Self.T.__fromKotlinPointer(other))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_Number_toByte__reverse_swift")
package func kotlin_Number_toByte__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int8 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int8 = _self.toByte()
    return _result
}

@available(*, deprecated, message: """
Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.
If you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.
See https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration. Replacement: this.toInt().toChar()
""")
@_cdecl("kotlin_Number_toChar__reverse_swift")
package func kotlin_Number_toChar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UInt16 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Unicode.UTF16.CodeUnit = _self.toChar()
    return _result
}

@_cdecl("kotlin_Number_toDouble__reverse_swift")
package func kotlin_Number_toDouble__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Double {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Double = _self.toDouble()
    return _result
}

@_cdecl("kotlin_Number_toFloat__reverse_swift")
package func kotlin_Number_toFloat__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Float {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Float = _self.toFloat()
    return _result
}

@_cdecl("kotlin_Number_toInt__reverse_swift")
package func kotlin_Number_toInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.toInt()
    return _result
}

@_cdecl("kotlin_Number_toLong__reverse_swift")
package func kotlin_Number_toLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int64 = _self.toLong()
    return _result
}

@_cdecl("kotlin_Number_toShort__reverse_swift")
package func kotlin_Number_toShort__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int16 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int16 = _self.toShort()
    return _result
}

@_cdecl("kotlin_Throwable_cause_get__reverse_swift")
package func kotlin_Throwable_cause_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = _self.cause
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_Throwable_message_get__reverse_swift")
package func kotlin_Throwable_message_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String? {
    let _self = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<Swift.String> = _self.message
    return _result ?? nil
}

@_cdecl("kotlin_collections_BooleanIterator_nextBoolean__reverse_swift")
package func kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlin.collections.BooleanIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.nextBoolean()
    return _result
}

@_cdecl("kotlin_collections_ByteIterator_nextByte__reverse_swift")
package func kotlin_collections_ByteIterator_nextByte__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int8 {
    let _self = ExportedKotlinPackages.kotlin.collections.ByteIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int8 = _self.nextByte()
    return _result
}

@_cdecl("kotlin_collections_CharIterator_nextChar__reverse_swift")
package func kotlin_collections_CharIterator_nextChar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UInt16 {
    let _self = ExportedKotlinPackages.kotlin.collections.CharIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Unicode.UTF16.CodeUnit = _self.nextChar()
    return _result
}

@_cdecl("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_Collection_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_isEmpty__reverse_swift")
package func kotlin_collections_Collection_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_iterator__reverse_swift")
package func kotlin_collections_Collection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_size_get__reverse_swift")
package func kotlin_collections_Collection_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_DoubleIterator_nextDouble__reverse_swift")
package func kotlin_collections_DoubleIterator_nextDouble__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Double {
    let _self = ExportedKotlinPackages.kotlin.collections.DoubleIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Double = _self.nextDouble()
    return _result
}

@_cdecl("kotlin_collections_FloatIterator_nextFloat__reverse_swift")
package func kotlin_collections_FloatIterator_nextFloat__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Float {
    let _self = ExportedKotlinPackages.kotlin.collections.FloatIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Float = _self.nextFloat()
    return _result
}

@_cdecl("kotlin_collections_IntIterator_nextInt__reverse_swift")
package func kotlin_collections_IntIterator_nextInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.nextInt()
    return _result
}

@_cdecl("kotlin_collections_Iterable_iterator__reverse_swift")
package func kotlin_collections_Iterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterable)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterable>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.T> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_hasNext__reverse_swift")
package func kotlin_collections_Iterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_next__reverse_swift")
package func kotlin_collections_Iterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_LongIterator_nextLong__reverse_swift")
package func kotlin_collections_LongIterator_nextLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = ExportedKotlinPackages.kotlin.collections.LongIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int64 = _self.nextLong()
    return _result
}

@_cdecl("kotlin_collections_Map_Entry_key_get__reverse_swift")
package func kotlin_collections_Map_Entry_key_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry)
    func _open<__Self: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.K = _self.key
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_Entry_value_get__reverse_swift")
package func kotlin_collections_Map_Entry_value_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry)
    func _open<__Self: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.V = _self.value
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_containsKey__TypesOfArguments__K_Swift_Hashable____reverse_swift")
package func kotlin_collections_Map_containsKey__TypesOfArguments__K_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsKey(key: __Self.K.__fromKotlinPointer(key))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_containsValue__TypesOfArguments__V____reverse_swift")
package func kotlin_collections_Map_containsValue__TypesOfArguments__V____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsValue(value: __Self.V.__fromKotlinPointer(value))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_entries_get__reverse_swift")
package func kotlin_collections_Map_entries_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<__Self.K,__Self.V>> = _self.entries
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_get__TypesOfArguments__K_Swift_Hashable____reverse_swift")
package func kotlin_collections_Map_get__TypesOfArguments__K_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<__Self.V> = _self._get(key: __Self.K.__fromKotlinPointer(key))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_isEmpty__reverse_swift")
package func kotlin_collections_Map_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_keys_get__reverse_swift")
package func kotlin_collections_Map_keys_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Set<__Self.K> = _self.keys
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_size_get__reverse_swift")
package func kotlin_collections_Map_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_values_get__reverse_swift")
package func kotlin_collections_Map_values_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Collection<__Self.V> = _self.values
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ShortIterator_nextShort__reverse_swift")
package func kotlin_collections_ShortIterator_nextShort__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int16 {
    let _self = ExportedKotlinPackages.kotlin.collections.ShortIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int16 = _self.nextShort()
    return _result
}

@_cdecl("kotlin_ranges_CharProgression_isEmpty__reverse_swift")
package func kotlin_ranges_CharProgression_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlin.ranges.CharProgression.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_ranges_CharProgression_iterator__reverse_swift")
package func kotlin_ranges_CharProgression_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlin.ranges.CharProgression.__createClassWrapper(externalRCRef: `self`)
    let _result: ExportedKotlinPackages.kotlin.collections.CharIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift")
package func kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(value: __Self.T.__fromKotlinPointer(value))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift")
package func kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.endInclusive
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_ClosedRange_isEmpty__reverse_swift")
package func kotlin_ranges_ClosedRange_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_ClosedRange_start_get__reverse_swift")
package func kotlin_ranges_ClosedRange_start_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.start
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_OpenEndRange_contains__TypesOfArguments__T____reverse_swift")
package func kotlin_ranges_OpenEndRange_contains__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.OpenEndRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.OpenEndRange>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(value: __Self.T.__fromKotlinPointer(value))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_OpenEndRange_endExclusive_get__reverse_swift")
package func kotlin_ranges_OpenEndRange_endExclusive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.OpenEndRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.OpenEndRange>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.endExclusive
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_OpenEndRange_isEmpty__reverse_swift")
package func kotlin_ranges_OpenEndRange_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.OpenEndRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.OpenEndRange>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_OpenEndRange_start_get__reverse_swift")
package func kotlin_ranges_OpenEndRange_start_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.OpenEndRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.OpenEndRange>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.start
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_UIntProgression_isEmpty__reverse_swift")
package func kotlin_ranges_UIntProgression_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlin.ranges.UIntProgression.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_ranges_ULongProgression_isEmpty__reverse_swift")
package func kotlin_ranges_ULongProgression_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlin.ranges.ULongProgression.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Array_get__TypesOfArguments__Swift_Int32__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_iterator(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Array_iterator(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32, _ _p2: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    return kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_size_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    return kotlin_Array_size_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Pair_copy__TypesOfArguments__A_B__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer?, _ _p2: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Pair_copy__TypesOfArguments__A_B__(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Pair_first_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Pair_first_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Pair_second_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Pair_second_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Triple_copy__TypesOfArguments__A_B_C__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer?, _ _p2: Swift.UnsafeMutableRawPointer?, _ _p3: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Triple_copy__TypesOfArguments__A_B_C__(_p0, _p1, _p2, _p3)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Triple_first_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Triple_first_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Triple_second_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Triple_second_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Triple_third_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Triple_third_get(_p0)
}
