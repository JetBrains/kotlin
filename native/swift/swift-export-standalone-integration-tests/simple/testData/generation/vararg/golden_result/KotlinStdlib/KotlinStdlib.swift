@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.collections {
    open class BooleanIterator: KotlinRuntime.KotlinBase {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public final func next() -> Swift.Bool {
            return kotlin_collections_BooleanIterator_next(self.__externalRCRef())
        }
        open func nextBoolean() -> Swift.Bool {
            return kotlin_collections_BooleanIterator_nextBoolean(self.__externalRCRef())
        }
    }
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
    public final class BooleanArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_BooleanArray_size_get(self.__externalRCRef())
            }
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Bool
        ) {
            fatalError()
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
            return kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self.__externalRCRef(), index, value)
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.BooleanIterator {
            return ExportedKotlinPackages.kotlin.collections.BooleanIterator.__createClassWrapper(externalRCRef: kotlin_BooleanArray_iterator(self.__externalRCRef()))
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
    public final class IntArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_IntArray_size_get(self.__externalRCRef())
            }
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int32
        ) {
            fatalError()
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
            return kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value)
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.IntIterator {
            return ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: kotlin_IntArray_iterator(self.__externalRCRef()))
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
    open class Number: KotlinRuntime.KotlinBase {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        open func toByte() -> Swift.Int8 {
            return kotlin_Number_toByte(self.__externalRCRef())
        }
        @available(*, deprecated, message: """
Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.
If you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.
See https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration. Replacement: this.toInt().toChar()
""")
        open func toChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_Number_toChar(self.__externalRCRef())
        }
        open func toDouble() -> Swift.Double {
            return kotlin_Number_toDouble(self.__externalRCRef())
        }
        open func toFloat() -> Swift.Float {
            return kotlin_Number_toFloat(self.__externalRCRef())
        }
        open func toInt() -> Swift.Int32 {
            return kotlin_Number_toInt(self.__externalRCRef())
        }
        open func toLong() -> Swift.Int64 {
            return kotlin_Number_toLong(self.__externalRCRef())
        }
        open func toShort() -> Swift.Int16 {
            return kotlin_Number_toShort(self.__externalRCRef())
        }
    }
}
