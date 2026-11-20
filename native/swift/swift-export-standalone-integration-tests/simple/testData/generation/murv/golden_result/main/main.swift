@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public protocol MarkedGreeter: KotlinRuntime.KotlinBase, main._MarkedGreeter {
    func greet(
        name: Swift.String
    ) -> Swift.String
    @discardableResult
    func greetIgnorable(
        name: Swift.String
    ) -> Swift.String
}
public protocol UnmarkedBase: KotlinRuntime.KotlinBase, main._UnmarkedBase {
    func unmarked() -> Swift.Int32
}
@objc(_main_MarkedGreeter)
public protocol _MarkedGreeter {
}
@objc(_main_UnmarkedBase)
public protocol _UnmarkedBase {
}
public protocol __MarkedGreeter: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __UnmarkedBase: KotlinRuntimeSupport._KotlinBridgeable {
}
public final class ClassInMarkedFile: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___ClassInMarkedFile_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ClassInMarkedFile_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func member() -> Swift.Int32 {
        return ClassInMarkedFile_member(self.__externalRCRef())
    }
}
public final class Greeter: KotlinRuntime.KotlinBase {
    public final class Companion: KotlinRuntime.KotlinBase {
        public static var shared: main.Greeter.Companion {
            get {
                return main.Greeter.Companion.__createClassWrapper(externalRCRef: Greeter_Companion_get())
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public func companionMember() -> Swift.Int32 {
            return Greeter_Companion_companionMember(self.__externalRCRef())
        }
    }
    public final class Nested: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Greeter_Nested_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { Greeter_Nested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public func nested() -> Swift.Int32 {
            return Greeter_Nested_nested(self.__externalRCRef())
        }
    }
    public var property: Swift.String {
        get {
            return Greeter_property_get(self.__externalRCRef())
        }
    }
    public override init() {
        let __kt = __root___Greeter_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Greeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func greet(
        name: Swift.String
    ) -> Swift.String {
        return Greeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    @discardableResult
    public func greetIgnorable(
        name: Swift.String
    ) -> Swift.String {
        return Greeter_greetIgnorable__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    public func greetUnit(
        name: Swift.String
    ) -> Swift.Void {
        return { Greeter_greetUnit__TypesOfArguments__Swift_String__(self.__externalRCRef(), name); return () }()
    }
}
open class LoudGreeter: KotlinRuntime.KotlinBase, main.MarkedGreeter, main.__MarkedGreeter {
    public override init() {
         let __kt: Swift.UnsafeMutableRawPointer!
         if Self.self == main.LoudGreeter.self {
             __kt = __root___LoudGreeter_init_allocate()
         } else {
             __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
         }
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___LoudGreeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func greet(
        name: Swift.String
    ) -> Swift.String {
        if Self.self == main.LoudGreeter.self {
            return LoudGreeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
        } else {
            return LoudGreeter_greet__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name)
        }
    }
    @discardableResult
    open func greetIgnorable(
        name: Swift.String
    ) -> Swift.String {
        if Self.self == main.LoudGreeter.self {
            return LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
        } else {
            return LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String___direct(self.__externalRCRef(), name)
        }
    }
}
public final class LouderGreeter: main.LoudGreeter {
    public override init() {
        let __kt = __root___LouderGreeter_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___LouderGreeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func greet(
        name: Swift.String
    ) -> Swift.String {
        return LouderGreeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
}
public final class MarkedObject: KotlinRuntime.KotlinBase {
    public static var shared: main.MarkedObject {
        get {
            return main.MarkedObject.__createClassWrapper(externalRCRef: __root___MarkedObject_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func member() -> Swift.Int32 {
        return MarkedObject_member(self.__externalRCRef())
    }
}
public final class MarkedOperators: KotlinRuntime.KotlinBase {
    public var value: Swift.Int32 {
        get {
            return MarkedOperators_value_get(self.__externalRCRef())
        }
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___MarkedOperators_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MarkedOperators_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public static func +(
        this: main.MarkedOperators,
        other: main.MarkedOperators
    ) -> main.MarkedOperators {
        this._plus(other: other)
    }
    public static func <(
        this: main.MarkedOperators,
        other: main.MarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: main.MarkedOperators,
        other: main.MarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: main.MarkedOperators,
        other: main.MarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: main.MarkedOperators,
        other: main.MarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
    public func _compareTo(
        other: main.MarkedOperators
    ) -> Swift.Int32 {
        return MarkedOperators_compareTo__TypesOfArguments__main_MarkedOperators__(self.__externalRCRef(), other.__externalRCRef())
    }
    public func _get(
        index: Swift.Int32
    ) -> Swift.Int32 {
        return MarkedOperators_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
    public func _plus(
        other: main.MarkedOperators
    ) -> main.MarkedOperators {
        return main.MarkedOperators.__createClassWrapper(externalRCRef: MarkedOperators_plus__TypesOfArguments__main_MarkedOperators__(self.__externalRCRef(), other.__externalRCRef()))
    }
    public func callAsFunction() -> Swift.Int32 {
        return MarkedOperators_invoke(self.__externalRCRef())
    }
    public subscript(
        index: Swift.Int32
    ) -> Swift.Int32 {
        get {
            _get(index: index)
        }
    }
}
public final class MarkedOverridesUnmarked: KotlinRuntime.KotlinBase, main.UnmarkedBase, main.__UnmarkedBase {
    public override init() {
        let __kt = __root___MarkedOverridesUnmarked_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MarkedOverridesUnmarked_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func unmarked() -> Swift.Int32 {
        return MarkedOverridesUnmarked_unmarked(self.__externalRCRef())
    }
}
public final class SilentGreeter: KotlinRuntime.KotlinBase, main.MarkedGreeter, main.__MarkedGreeter {
    public static var shared: main.SilentGreeter {
        get {
            return main.SilentGreeter.__createClassWrapper(externalRCRef: __root___SilentGreeter_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @discardableResult
    public func greet(
        name: Swift.String
    ) -> Swift.String {
        return SilentGreeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    @discardableResult
    public func greetIgnorable(
        name: Swift.String
    ) -> Swift.String {
        return SilentGreeter_greetIgnorable__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
}
public final class UnmarkedClass: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___UnmarkedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___UnmarkedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func member() -> Swift.Int32 {
        return UnmarkedClass_member(self.__externalRCRef())
    }
}
public final class UnmarkedOperators: KotlinRuntime.KotlinBase {
    public var value: Swift.Int32 {
        get {
            return UnmarkedOperators_value_get(self.__externalRCRef())
        }
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___UnmarkedOperators_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___UnmarkedOperators_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public static func +(
        this: main.UnmarkedOperators,
        other: main.UnmarkedOperators
    ) -> main.UnmarkedOperators {
        this._plus(other: other)
    }
    public static func <(
        this: main.UnmarkedOperators,
        other: main.UnmarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: main.UnmarkedOperators,
        other: main.UnmarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: main.UnmarkedOperators,
        other: main.UnmarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: main.UnmarkedOperators,
        other: main.UnmarkedOperators
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
    public func _compareTo(
        other: main.UnmarkedOperators
    ) -> Swift.Int32 {
        return UnmarkedOperators_compareTo__TypesOfArguments__main_UnmarkedOperators__(self.__externalRCRef(), other.__externalRCRef())
    }
    public func _get(
        index: Swift.Int32
    ) -> Swift.Int32 {
        return UnmarkedOperators_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
    }
    public func _plus(
        other: main.UnmarkedOperators
    ) -> main.UnmarkedOperators {
        return main.UnmarkedOperators.__createClassWrapper(externalRCRef: UnmarkedOperators_plus__TypesOfArguments__main_UnmarkedOperators__(self.__externalRCRef(), other.__externalRCRef()))
    }
    public func callAsFunction() -> Swift.Int32 {
        return UnmarkedOperators_invoke(self.__externalRCRef())
    }
    public subscript(
        index: Swift.Int32
    ) -> Swift.Int32 {
        get {
            _get(index: index)
        }
    }
}
@discardableResult
public func addAndIgnoreResult(
    _ receiver: any ExportedKotlinPackages.kotlin.collections.MutableList,
    element: (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> Swift.Bool {
    return __root___addAndIgnoreResult__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_MutableList_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
}
@discardableResult
public func ignorableInMarkedFile() -> Swift.Int32 {
    return __root___ignorableInMarkedFile()
}
public func markedByFile() -> Swift.Int32 {
    return __root___markedByFile()
}
public func markedExtension(
    _ receiver: Swift.Int32
) -> Swift.Int32 {
    return __root___markedExtension__TypesOfArgumentsE__Swift_Int32__(receiver)
}
public func markedNothing() -> Swift.Never {
    return { __root___markedNothing(); fatalError() }()
}
public func markedNullableNothing() -> Swift.Never? {
    return { __root___markedNullableNothing(); return nil }()
}
public func markedNullableUnit() -> Swift.Void? {
    return (__root___markedNullableUnit() ? () : nil)
}
public func markedUnit() -> Swift.Void {
    return { __root___markedUnit(); return () }()
}
public func unmarked() -> Swift.Int32 {
    return __root___unmarked()
}
public func unmarkedNothing() -> Swift.Never {
    return { __root___unmarkedNothing(); fatalError() }()
}
public func unmarkedNullableNothing() -> Swift.Never? {
    return { __root___unmarkedNullableNothing(); return nil }()
}
public func unmarkedUnit() -> Swift.Void {
    return { __root___unmarkedUnit(); return () }()
}
extension Swift.Int32 {
    public func markedExtension() -> Self {
        let receiver = self
        return main.markedExtension(receiver)
    }
}
@_documentation(visibility: internal)
extension main.MarkedGreeter where Self : main.__MarkedGreeter {
    public func greet(
        name: Swift.String
    ) -> Swift.String {
        return MarkedGreeter_greet__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
    @discardableResult
    public func greetIgnorable(
        name: Swift.String
    ) -> Swift.String {
        return MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String__(self.__externalRCRef(), name)
    }
}
extension main.MarkedGreeter {
}
extension ExportedKotlinPackages.kotlin.collections.MutableList {
    @discardableResult
    public func addAndIgnoreResult(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        let receiver = self
        return main.addAndIgnoreResult(receiver, element: element)
    }
}
@_documentation(visibility: internal)
extension main.UnmarkedBase where Self : main.__UnmarkedBase {
    public func unmarked() -> Swift.Int32 {
        return UnmarkedBase_unmarked(self.__externalRCRef())
    }
}
extension main.UnmarkedBase {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.MarkedGreeter, main.__MarkedGreeter where Wrapped : main._MarkedGreeter {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.UnmarkedBase, main.__UnmarkedBase where Wrapped : main._UnmarkedBase {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._MarkedGreeter {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._UnmarkedBase {
}
@_cdecl("LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift")
package func LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = main.LoudGreeter.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.String = _self.greetIgnorable(name: name)
    return _result
}

@_cdecl("LoudGreeter_greet__TypesOfArguments__Swift_String____reverse_swift")
package func LoudGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = main.LoudGreeter.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.String = _self.greet(name: name)
    return _result
}

@_cdecl("MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift")
package func MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.MarkedGreeter.Type.self) as! any main.MarkedGreeter
    let _result: Swift.String = _self.greetIgnorable(name: name)
    return _result
}

@_cdecl("MarkedGreeter_greet__TypesOfArguments__Swift_String____reverse_swift")
package func MarkedGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ name: Swift.String) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.MarkedGreeter.Type.self) as! any main.MarkedGreeter
    let _result: Swift.String = _self.greet(name: name)
    return _result
}

@_cdecl("UnmarkedBase_unmarked__reverse_swift")
package func UnmarkedBase_unmarked__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: main.UnmarkedBase.Type.self) as! any main.UnmarkedBase
    let _result: Swift.Int32 = _self.unmarked()
    return _result
}
