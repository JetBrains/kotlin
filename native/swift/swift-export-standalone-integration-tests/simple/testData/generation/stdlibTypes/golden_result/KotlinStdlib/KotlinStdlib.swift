@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.text.Appendable where Self : KotlinRuntimeSupport._KotlinBridged {
    public func append(
        value: Swift.Unicode.UTF16.CodeUnit
    ) -> any ExportedKotlinPackages.kotlin.text.Appendable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_text_Appendable_append__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value)) as! any ExportedKotlinPackages.kotlin.text.Appendable
    }
    public func append(
        value: (any ExportedKotlinPackages.kotlin.CharSequence)?
    ) -> any ExportedKotlinPackages.kotlin.text.Appendable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_text_Appendable_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlin.text.Appendable
    }
    public func append(
        value: (any ExportedKotlinPackages.kotlin.CharSequence)?,
        startIndex: Swift.Int32,
        endIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.text.Appendable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_text_Appendable_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence__Swift_Int32_Swift_Int32__(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.text.Appendable
    }
}
extension ExportedKotlinPackages.kotlin.CharSequence where Self : KotlinRuntimeSupport._KotlinBridged {
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.CharSequence
    }
    public subscript(
        index: Swift.Int32
    ) -> Swift.Unicode.UTF16.CodeUnit {
        get {
            _get(index: index)
        }
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.CharSequence where Wrapped : ExportedKotlinPackages.kotlin._CharSequence {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.text.Appendable where Wrapped : ExportedKotlinPackages.kotlin.text._Appendable {
}
extension ExportedKotlinPackages.kotlin.collections {
    open class ByteIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public final func next() -> Swift.Int8 {
            return kotlin_collections_ByteIterator_next(self.__externalRCRef())
        }
        open func nextByte() -> Swift.Int8 {
            return kotlin_collections_ByteIterator_nextByte(self.__externalRCRef())
        }
    }
    open class CharIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public final func next() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_collections_CharIterator_next(self.__externalRCRef())
        }
        open func nextChar() -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_collections_CharIterator_nextChar(self.__externalRCRef())
        }
    }
}
extension ExportedKotlinPackages.kotlin {
    public protocol CharSequence: KotlinRuntime.KotlinBase {
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
    @objc(_CharSequence)
    package protocol _CharSequence {
    }
    public final class ByteArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_ByteArray_size_get(self.__externalRCRef())
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
            `init`: @escaping (Swift.Int32) -> Swift.Int8
        ) {
            fatalError()
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
            return kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self.__externalRCRef(), index, value)
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.ByteIterator {
            return ExportedKotlinPackages.kotlin.collections.ByteIterator.__createClassWrapper(externalRCRef: kotlin_ByteArray_iterator(self.__externalRCRef()))
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
    public final class CharArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_CharArray_size_get(self.__externalRCRef())
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
            `init`: @escaping (Swift.Int32) -> Swift.Unicode.UTF16.CodeUnit
        ) {
            fatalError()
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
            return kotlin_CharArray_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), index, value)
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.CharIterator {
            return ExportedKotlinPackages.kotlin.collections.CharIterator.__createClassWrapper(externalRCRef: kotlin_CharArray_iterator(self.__externalRCRef()))
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
}
extension ExportedKotlinPackages.kotlin.text {
    public protocol Appendable: KotlinRuntime.KotlinBase {
        func append(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> any ExportedKotlinPackages.kotlin.text.Appendable
        func append(
            value: (any ExportedKotlinPackages.kotlin.CharSequence)?
        ) -> any ExportedKotlinPackages.kotlin.text.Appendable
        func append(
            value: (any ExportedKotlinPackages.kotlin.CharSequence)?,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.text.Appendable
    }
    @objc(_Appendable)
    package protocol _Appendable {
    }
    public final class StringBuilder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.CharSequence, ExportedKotlinPackages.kotlin._CharSequence, ExportedKotlinPackages.kotlin.text.Appendable, ExportedKotlinPackages.kotlin.text._Appendable, KotlinRuntimeSupport._KotlinBridged {
        public var length: Swift.Int32 {
            get {
                return kotlin_text_StringBuilder_length_get(self.__externalRCRef())
            }
        }
        public init() {
            if Self.self != ExportedKotlinPackages.kotlin.text.StringBuilder.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.text.StringBuilder ") }
            let __kt = kotlin_text_StringBuilder_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        public init(
            capacity: Swift.Int32
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.text.StringBuilder.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.text.StringBuilder ") }
            let __kt = kotlin_text_StringBuilder_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, capacity)
        }
        public init(
            content: any ExportedKotlinPackages.kotlin.CharSequence
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.text.StringBuilder.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.text.StringBuilder ") }
            let __kt = kotlin_text_StringBuilder_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_CharSequence__(__kt, content.__externalRCRef())
        }
        public init(
            content: Swift.String
        ) {
            if Self.self != ExportedKotlinPackages.kotlin.text.StringBuilder.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlin.text.StringBuilder ") }
            let __kt = kotlin_text_StringBuilder_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, content)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Unicode.UTF16.CodeUnit {
            return kotlin_text_StringBuilder_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> Swift.Void {
            return kotlin_text_StringBuilder_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), index, value)
        }
        public func append(
            value: Swift.Bool
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Bool__(self.__externalRCRef(), value))
        }
        public func append(
            value: ExportedKotlinPackages.kotlin.CharArray
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray__(self.__externalRCRef(), value.__externalRCRef()))
        }
        public func append(
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), value))
        }
        public func append(
            value: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Double__(self.__externalRCRef(), value))
        }
        public func append(
            value: Swift.Float
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Float__(self.__externalRCRef(), value))
        }
        public func append(
            value: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int16__(self.__externalRCRef(), value))
        }
        public func append(
            value: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value))
        }
        public func append(
            value: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), value))
        }
        public func append(
            value: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int8__(self.__externalRCRef(), value))
        }
        public func append(
            value: (any ExportedKotlinPackages.kotlin.CharSequence)?
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil))
        }
        public func append(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.intoRCRefUnsafe() } ?? nil))
        }
        public func append(
            value: Swift.String?
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_Swift_String___(self.__externalRCRef(), value ?? nil))
        }
        public func append(
            value: (any ExportedKotlinPackages.kotlin.CharSequence)?,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence__Swift_Int32_Swift_Int32__(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, startIndex, endIndex))
        }
        public func appendRange(
            value: any ExportedKotlinPackages.kotlin.CharSequence,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_appendRange__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_CharSequence_Swift_Int32_Swift_Int32__(self.__externalRCRef(), value.__externalRCRef(), startIndex, endIndex))
        }
        public func appendRange(
            value: ExportedKotlinPackages.kotlin.CharArray,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_appendRange__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32__(self.__externalRCRef(), value.__externalRCRef(), startIndex, endIndex))
        }
        public func capacity() -> Swift.Int32 {
            return kotlin_text_StringBuilder_capacity(self.__externalRCRef())
        }
        public func deleteAt(
            index: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_deleteAt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
        }
        public func deleteRange(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_deleteRange__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex))
        }
        public func ensureCapacity(
            minimumCapacity: Swift.Int32
        ) -> Swift.Void {
            return kotlin_text_StringBuilder_ensureCapacity__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), minimumCapacity)
        }
        public func indexOf(
            string: Swift.String
        ) -> Swift.Int32 {
            return kotlin_text_StringBuilder_indexOf__TypesOfArguments__Swift_String__(self.__externalRCRef(), string)
        }
        public func indexOf(
            string: Swift.String,
            startIndex: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_text_StringBuilder_indexOf__TypesOfArguments__Swift_String_Swift_Int32__(self.__externalRCRef(), string, startIndex)
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Bool
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Bool__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: ExportedKotlinPackages.kotlin.CharArray
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlin_CharArray__(self.__externalRCRef(), index, value.__externalRCRef()))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Unicode.UTF16.CodeUnit
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Double
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Double__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Float
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Float__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Int16
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int16__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Int64
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int64__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.Int8
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int8__(self.__externalRCRef(), index, value))
        }
        public func insert(
            index: Swift.Int32,
            value: (any ExportedKotlinPackages.kotlin.CharSequence)?
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil))
        }
        public func insert(
            index: Swift.Int32,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, value.map { it in it.intoRCRefUnsafe() } ?? nil))
        }
        public func insert(
            index: Swift.Int32,
            value: Swift.String?
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_Swift_String___(self.__externalRCRef(), index, value ?? nil))
        }
        public func insertRange(
            index: Swift.Int32,
            value: any ExportedKotlinPackages.kotlin.CharSequence,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insertRange__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_CharSequence_Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value.__externalRCRef(), startIndex, endIndex))
        }
        public func insertRange(
            index: Swift.Int32,
            value: ExportedKotlinPackages.kotlin.CharArray,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_insertRange__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value.__externalRCRef(), startIndex, endIndex))
        }
        public func lastIndexOf(
            string: Swift.String
        ) -> Swift.Int32 {
            return kotlin_text_StringBuilder_lastIndexOf__TypesOfArguments__Swift_String__(self.__externalRCRef(), string)
        }
        public func lastIndexOf(
            string: Swift.String,
            startIndex: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_text_StringBuilder_lastIndexOf__TypesOfArguments__Swift_String_Swift_Int32__(self.__externalRCRef(), string, startIndex)
        }
        public func reverse() -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_reverse(self.__externalRCRef()))
        }
        public func setLength(
            newLength: Swift.Int32
        ) -> Swift.Void {
            return kotlin_text_StringBuilder_setLength__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newLength)
        }
        public func setRange(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32,
            value: Swift.String
        ) -> ExportedKotlinPackages.kotlin.text.StringBuilder {
            return ExportedKotlinPackages.kotlin.text.StringBuilder.__createClassWrapper(externalRCRef: kotlin_text_StringBuilder_setRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_String__(self.__externalRCRef(), startIndex, endIndex, value))
        }
        public func subSequence(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.CharSequence {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_text_StringBuilder_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)) as! any ExportedKotlinPackages.kotlin.CharSequence
        }
        public func substring(
            startIndex: Swift.Int32
        ) -> Swift.String {
            return kotlin_text_StringBuilder_substring__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), startIndex)
        }
        public func substring(
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> Swift.String {
            return kotlin_text_StringBuilder_substring__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), startIndex, endIndex)
        }
        public func toCharArray(
            destination: ExportedKotlinPackages.kotlin.CharArray,
            destinationOffset: Swift.Int32,
            startIndex: Swift.Int32,
            endIndex: Swift.Int32
        ) -> Swift.Void {
            return kotlin_text_StringBuilder_toCharArray__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32_Swift_Int32__(self.__externalRCRef(), destination.__externalRCRef(), destinationOffset, startIndex, endIndex)
        }
        public func toString() -> Swift.String {
            return kotlin_text_StringBuilder_toString(self.__externalRCRef())
        }
        public func trimToSize() -> Swift.Void {
            return kotlin_text_StringBuilder_trimToSize(self.__externalRCRef())
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
}
