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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
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
    }
    open class IntIterator: KotlinRuntime.KotlinBase {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
            return { kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self.__externalRCRef(), index, value); return () }()
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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
            return { kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value); return () }()
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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func toByte() -> Swift.Int8 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toByte(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toByte': a Swift subclass must override it and must not call super.")
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
        open func toInt() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toInt(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toInt': a Swift subclass must override it and must not call super.")
            }
        }
        open func toLong() -> Swift.Int64 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toLong(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toLong': a Swift subclass must override it and must not call super.")
            }
        }
        open func toShort() -> Swift.Int16 {
            if Self.self == ExportedKotlinPackages.kotlin.Number.self {
                return kotlin_Number_toShort(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.Number.toShort': a Swift subclass must override it and must not call super.")
            }
        }
    }
}
@_cdecl("kotlin_Number_toByte__reverse_swift")
package func kotlin_Number_toByte__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int8 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
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
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Unicode.UTF16.CodeUnit = _self.toChar()
    return _result
}

@_cdecl("kotlin_Number_toDouble__reverse_swift")
package func kotlin_Number_toDouble__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Double {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Double = _self.toDouble()
    return _result
}

@_cdecl("kotlin_Number_toFloat__reverse_swift")
package func kotlin_Number_toFloat__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Float {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Float = _self.toFloat()
    return _result
}

@_cdecl("kotlin_Number_toInt__reverse_swift")
package func kotlin_Number_toInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.toInt()
    return _result
}

@_cdecl("kotlin_Number_toLong__reverse_swift")
package func kotlin_Number_toLong__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int64 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int64 = _self.toLong()
    return _result
}

@_cdecl("kotlin_Number_toShort__reverse_swift")
package func kotlin_Number_toShort__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int16 {
    let _self = ExportedKotlinPackages.kotlin.Number.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int16 = _self.toShort()
    return _result
}

@_cdecl("kotlin_collections_BooleanIterator_nextBoolean__reverse_swift")
package func kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlin.collections.BooleanIterator.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.nextBoolean()
    return _result
}

@_cdecl("kotlin_collections_IntIterator_nextInt__reverse_swift")
package func kotlin_collections_IntIterator_nextInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.nextInt()
    return _result
}
