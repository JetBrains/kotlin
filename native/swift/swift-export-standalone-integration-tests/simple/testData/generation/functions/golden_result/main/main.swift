@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public final class Foo: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func ext(
        _ receiver: Swift.String
    ) -> Swift.Void {
        return Foo_ext__TypesOfArguments__Swift_String__(self.__externalRCRef(), receiver)
    }
    public func getExtVal(
        _ receiver: Swift.String
    ) -> Swift.String {
        return Foo_extVal_get__TypesOfArguments__Swift_String__(self.__externalRCRef(), receiver)
    }
    public func getExtVar(
        _ receiver: Swift.String
    ) -> Swift.String {
        return Foo_extVar_get__TypesOfArguments__Swift_String__(self.__externalRCRef(), receiver)
    }
    public func setExtVar(
        _ receiver: Swift.String,
        v: Swift.String
    ) -> Swift.Void {
        return Foo_extVar_set__TypesOfArguments__Swift_String_Swift_String__(self.__externalRCRef(), receiver, v)
    }
}
public func foo(
    _ receiver: Swift.Int32
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Int32__(receiver)
}
public func foo(
    _ receiver: Swift.Int32?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func foo(
    _ receiver: main.Foo
) -> Swift.Void {
    return __root___foo__TypesOfArguments__main_Foo__(receiver.__externalRCRef())
}
public func foo(
    _ receiver: main.Foo?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_main_Foo___(receiver.map { it in it.__externalRCRef() } ?? nil)
}
public func foo() -> Swift.Int32 {
    return __root___foo()
}
public func getBar(
    _ receiver: Swift.Int32
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__Swift_Int32__(receiver)
}
public func getBar(
    _ receiver: Swift.Int32?
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func getBar(
    _ receiver: main.Foo
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__main_Foo__(receiver.__externalRCRef())
}
public func getBar(
    _ receiver: main.Foo?
) -> Swift.String {
    return __root___bar_get__TypesOfArguments__Swift_Optional_main_Foo___(receiver.map { it in it.__externalRCRef() } ?? nil)
}
public func getFoo(
    _ receiver: Swift.Int32
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__Swift_Int32__(receiver)
}
public func getFoo(
    _ receiver: Swift.Int32?
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func getFoo(
    _ receiver: main.Foo
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__main_Foo__(receiver.__externalRCRef())
}
public func getFoo(
    _ receiver: main.Foo?
) -> Swift.String {
    return __root___foo_get__TypesOfArguments__Swift_Optional_main_Foo___(receiver.map { it in it.__externalRCRef() } ?? nil)
}
public func return_any_should_append_runtime_import() -> any KotlinRuntimeSupport._KotlinBridgeable {
    return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: __root___return_any_should_append_runtime_import())
}
public func setFoo(
    _ receiver: Swift.Int32,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__Swift_Int32_Swift_String__(receiver, v)
}
public func setFoo(
    _ receiver: Swift.Int32?,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__Swift_Optional_Swift_Int32__Swift_String__(receiver.map { it in NSNumber(value: it) } ?? nil, v)
}
public func setFoo(
    _ receiver: main.Foo,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__main_Foo_Swift_String__(receiver.__externalRCRef(), v)
}
public func setFoo(
    _ receiver: main.Foo?,
    v: Swift.String
) -> Swift.Void {
    return __root___foo_set__TypesOfArguments__Swift_Optional_main_Foo__Swift_String__(receiver.map { it in it.__externalRCRef() } ?? nil, v)
}
extension ExportedKotlinPackages.inline {
    public final class Bar: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.inline.Bar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.inline.Bar ") }
            let __kt = inline_Bar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            inline_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func bar() -> Swift.String {
            return inline_Bar_bar(self.__externalRCRef())
        }
    }
    public static func foo() -> Swift.String {
        return inline_foo()
    }
}
extension ExportedKotlinPackages.namespace1.local_functions {
    public static func foo() -> Swift.Void {
        return namespace1_local_functions_foo()
    }
}
extension ExportedKotlinPackages.namespace1.main {
    public static func all_args(
        arg1: Swift.Bool,
        arg2: Swift.Int8,
        arg3: Swift.Int16,
        arg4: Swift.Int32,
        arg5: Swift.Int64,
        arg6: Swift.UInt8,
        arg7: Swift.UInt16,
        arg8: Swift.UInt32,
        arg9: Swift.UInt64,
        arg10: Swift.Float,
        arg11: Swift.Double,
        arg12: Swift.Unicode.UTF16.CodeUnit
    ) -> Swift.Void {
        return namespace1_main_all_args__TypesOfArguments__Swift_Bool_Swift_Int8_Swift_Int16_Swift_Int32_Swift_Int64_Swift_UInt8_Swift_UInt16_Swift_UInt32_Swift_UInt64_Swift_Float_Swift_Double_Swift_Unicode_UTF16_CodeUnit__(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12)
    }
    public static func foobar(
        param: Swift.Int32
    ) -> Swift.Int32 {
        return namespace1_main_foobar__TypesOfArguments__Swift_Int32__(param)
    }
}
extension ExportedKotlinPackages.namespace1 {
    public static func bar() -> Swift.Int32 {
        return namespace1_bar()
    }
}
extension ExportedKotlinPackages.namespace2 {
    public static func foo(
        arg1: Swift.Int32
    ) -> Swift.Int32 {
        return namespace2_foo__TypesOfArguments__Swift_Int32__(arg1)
    }
}
extension ExportedKotlinPackages.namespace3 {
    public static var bar: Swift.Void {
        get {
            return namespace3_bar_get()
        }
    }
    public static func foo(
        faux: Swift.Void
    ) -> Swift.Void {
        return namespace3_foo__TypesOfArguments__Swift_Void__()
    }
    public static func foo(
        arg1: Swift.Int32,
        faux: Swift.Void
    ) -> Swift.Void {
        return namespace3_foo__TypesOfArguments__Swift_Int32_Swift_Void__(arg1)
    }
}
extension ExportedKotlinPackages.operators {
    public final class Foo: KotlinRuntime.KotlinBase {
        public final class EmptyIterator: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.operators.Foo.EmptyIterator {
                get {
                    return ExportedKotlinPackages.operators.Foo.EmptyIterator.__createClassWrapper(externalRCRef: operators_Foo_EmptyIterator_get())
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
            public func hasNext() -> Swift.Bool {
                return operators_Foo_EmptyIterator_hasNext(self.__externalRCRef())
            }
            public func next() -> Swift.Int32 {
                return operators_Foo_EmptyIterator_next(self.__externalRCRef())
            }
        }
        public var value: Swift.Int32 {
            get {
                return operators_Foo_value_get(self.__externalRCRef())
            }
            set {
                return operators_Foo_value_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
            }
        }
        public init(
            value: Swift.Int32
        ) {
            if Self.self != ExportedKotlinPackages.operators.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.operators.Foo ") }
            let __kt = operators_Foo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            operators_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public static prefix func !(
            this: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._not()
        }
        public static func %(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._rem(other: other)
        }
        public static func %=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            this._remAssign(other: other)
        }
        public static func *(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._times(other: other)
        }
        public static func *=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            this._timesAssign(other: other)
        }
        public static prefix func +(
            this: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._unaryPlus()
        }
        public static func +(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._plus(other: other)
        }
        public static func +=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            this._plusAssign(other: other)
        }
        public static prefix func -(
            this: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._unaryMinus()
        }
        public static func -(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._minus(other: other)
        }
        public static func -=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            this._minusAssign(other: other)
        }
        public static func /(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            this._div(other: other)
        }
        public static func /=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            this._divAssign(other: other)
        }
        public static func <(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func ==(
            this: ExportedKotlinPackages.operators.Foo,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public static func >(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public func _compareTo(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Int32 {
            return operators_Foo_compareTo__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _div(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_div__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _divAssign(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            return operators_Foo_divAssign__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int32 {
            return operators_Foo_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _minus(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_minus__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _minusAssign(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            return operators_Foo_minusAssign__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _not() -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_not(self.__externalRCRef()))
        }
        public func _plus(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_plus__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _plusAssign(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            return operators_Foo_plusAssign__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _rem(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_rem__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _remAssign(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            return operators_Foo_remAssign__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return operators_Foo_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value)
        }
        public func _times(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_times__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func _timesAssign(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Void {
            return operators_Foo_timesAssign__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func _unaryMinus() -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_unaryMinus(self.__externalRCRef()))
        }
        public func _unaryPlus() -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_unaryPlus(self.__externalRCRef()))
        }
        public func callAsFunction() -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_invoke(self.__externalRCRef()))
        }
        public func contains(
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Bool {
            return operators_Foo_contains__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func copy(
            value: Swift.Int32
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_copy__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), value))
        }
        public func dec() -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_dec(self.__externalRCRef()))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return operators_Foo_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return operators_Foo_hashCode(self.__externalRCRef())
        }
        public func inc() -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_inc(self.__externalRCRef()))
        }
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: operators_Foo_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        public func rangeTo(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_rangeTo__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func rangeUntil(
            other: ExportedKotlinPackages.operators.Foo
        ) -> ExportedKotlinPackages.operators.Foo {
            return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_Foo_rangeUntil__TypesOfArguments__ExportedKotlinPackages_operators_Foo__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func toString() -> Swift.String {
            return operators_Foo_toString(self.__externalRCRef())
        }
        public static func ~=(
            this: ExportedKotlinPackages.operators.Foo,
            other: ExportedKotlinPackages.operators.Foo
        ) -> Swift.Bool {
            this.contains(other: other)
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
    public static func invoke(
        _ receiver: ExportedKotlinPackages.operators.Foo,
        other: ExportedKotlinPackages.operators.Foo
    ) -> ExportedKotlinPackages.operators.Foo {
        return ExportedKotlinPackages.operators.Foo.__createClassWrapper(externalRCRef: operators_invoke__TypesOfArguments__ExportedKotlinPackages_operators_Foo_ExportedKotlinPackages_operators_Foo__(receiver.__externalRCRef(), other.__externalRCRef()))
    }
}
extension ExportedKotlinPackages.overload {
    public final class Foo: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.overload.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.overload.Foo ") }
            let __kt = overload_Foo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            overload_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static func foo(
        arg1: Swift.Double
    ) -> Swift.Int32 {
        return overload_foo__TypesOfArguments__Swift_Double__(arg1)
    }
    public static func foo(
        arg1: ExportedKotlinPackages.overload.Foo
    ) -> Swift.Void {
        return overload_foo__TypesOfArguments__ExportedKotlinPackages_overload_Foo__(arg1.__externalRCRef())
    }
    public static func foo(
        arg1: Swift.Int32
    ) -> Swift.Int32 {
        return overload_foo__TypesOfArguments__Swift_Int32__(arg1)
    }
    public static func foo(
        arg1: ExportedKotlinPackages.overload.Foo?
    ) -> Swift.Void {
        return overload_foo__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_overload_Foo___(arg1.map { it in it.__externalRCRef() } ?? nil)
    }
}
