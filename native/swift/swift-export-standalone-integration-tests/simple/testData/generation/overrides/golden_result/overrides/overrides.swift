@_implementationOnly import KotlinBridges_overrides
import KotlinRuntime
import KotlinRuntimeSupport

open class AbstractBase: KotlinRuntime.KotlinBase {
    open var abstractVal: Swift.Int32 {
        get {
            return AbstractBase_abstractVal_get(self.__externalRCRef())
        }
    }
    package init() {
        fatalError()
    }
    package init(
        x: Swift.Int32
    ) {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func abstractFun1() -> Swift.Void {
        return { AbstractBase_abstractFun1(self.__externalRCRef()); return () }()
    }
    open func abstractFun2() -> Swift.Void {
        return { AbstractBase_abstractFun2(self.__externalRCRef()); return () }()
    }
}
open class AbstractDerived2: overrides.OpenDerived1 {
    package override init() {
        fatalError()
    }
    package override init(
        x: Swift.Int32
    ) {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open override func abstractFun1() -> Swift.Void {
        return { AbstractDerived2_abstractFun1(self.__externalRCRef()); return () }()
    }
}
open class Child: overrides.Parent {
    open override var objectOptionalVar: overrides.Parent? {
        get {
            return { switch Child_objectOptionalVar_get(self.__externalRCRef()) { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    open override var objectVar: overrides.Parent {
        get {
            return overrides.Parent.__createClassWrapper(externalRCRef: Child_objectVar_get(self.__externalRCRef()))
        }
    }
    open override var primitiveTypeVar: Swift.Int32 {
        get {
            return Child_primitiveTypeVar_get(self.__externalRCRef())
        }
    }
    open override var subtypeObjectVar: overrides.Child {
        get {
            return overrides.Child.__createClassWrapper(externalRCRef: Child_subtypeObjectVar_get(self.__externalRCRef()))
        }
    }
    open override var subtypeOptionalObjectVar: overrides.Child {
        get {
            return overrides.Child.__createClassWrapper(externalRCRef: Child_subtypeOptionalObjectVar_get(self.__externalRCRef()))
        }
    }
    open override var subtypeOptionalPrimitiveVar: Swift.Int32 {
        get {
            return Child_subtypeOptionalPrimitiveVar_get(self.__externalRCRef())
        }
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value); return () }()
    }
    public override init(
        value: Swift.String
    ) {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, value); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public init(
        nullable: Swift.Int32,
        poly: overrides.Parent,
        nullablePoly: overrides.Parent
    ) {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_overrides_Parent_overrides_Parent__(__kt, nullable, poly.__externalRCRef(), nullablePoly.__externalRCRef()); return () }()
    }
    public static func ==(
        this: overrides.Child,
        to: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.equals(to: to)
    }
    open override func actuallyOverride(
        nullable: Swift.Int32?,
        poly: overrides.Parent,
        nullablePoly: overrides.Parent?
    ) -> Swift.Void {
        return { Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent___(self.__externalRCRef(), nullable.map { it in NSNumber(value: it) } ?? nil, poly.__externalRCRef(), nullablePoly.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    open override func contains(
        element: Swift.Int32
    ) -> Swift.Bool {
        return Child_contains__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), element)
    }
    open override func equals(
        to: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), to.map { it in it.__externalRCRef() } ?? nil)
    }
    public final override func finalOverrideFunc() -> Swift.Void {
        return { Child_finalOverrideFunc(self.__externalRCRef()); return () }()
    }
    open func genericReturnTypeFunc() -> [overrides.Child] {
        return Child_genericReturnTypeFunc(self.__externalRCRef()) as! Swift.Array<overrides.Child>
    }
    open func nonoverride() -> Swift.Never {
        return { Child_nonoverride(self.__externalRCRef()); fatalError() }()
    }
    open override func objectFunc(
        arg: overrides.Child
    ) -> overrides.Parent {
        return overrides.Parent.__createClassWrapper(externalRCRef: Child_objectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open override func objectOptionalFunc(
        arg: overrides.Child
    ) -> overrides.Parent? {
        return { switch Child_objectOptionalFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()) { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }()
    }
    open override func overrideChainFunc() -> Swift.Void {
        return { Child_overrideChainFunc(self.__externalRCRef()); return () }()
    }
    open override func primitiveTypeFunc(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
    open override func subtypeObjectFunc(
        arg: overrides.Child
    ) -> overrides.Child {
        return overrides.Child.__createClassWrapper(externalRCRef: Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open override func subtypeOptionalObjectFunc() -> overrides.Child {
        return overrides.Child.__createClassWrapper(externalRCRef: Child_subtypeOptionalObjectFunc(self.__externalRCRef()))
    }
    open override func subtypeOptionalPrimitiveFunc() -> Swift.Int32 {
        return Child_subtypeOptionalPrimitiveFunc(self.__externalRCRef())
    }
    public static func ~=(
        this: overrides.Child,
        element: Swift.Int32
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
public final class GrandChild: overrides.Child {
    public override init(
        value: Swift.Int32
    ) {
        let __kt = __root___GrandChild_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___GrandChild_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func finalOverrideHopFunc() -> Swift.Void {
        return { GrandChild_finalOverrideHopFunc(self.__externalRCRef()); return () }()
    }
    public override func hopFunc() -> Swift.Void {
        return { GrandChild_hopFunc(self.__externalRCRef()); return () }()
    }
    public override func overrideChainFunc() -> Swift.Void {
        return { GrandChild_overrideChainFunc(self.__externalRCRef()); return () }()
    }
}
open class OpenDerived1: overrides.AbstractBase {
    open override var abstractVal: Swift.Int32 {
        get {
            return OpenDerived1_abstractVal_get(self.__externalRCRef())
        }
    }
    public override init() {
        let __kt = __root___OpenDerived1_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    public override init(
        x: Swift.Int32
    ) {
        let __kt = __root___OpenDerived1_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, x); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open override func abstractFun1() -> Swift.Void {
        return { OpenDerived1_abstractFun1(self.__externalRCRef()); return () }()
    }
    open override func abstractFun2() -> Swift.Void {
        return { OpenDerived1_abstractFun2(self.__externalRCRef()); return () }()
    }
}
open class Parent: KotlinRuntime.KotlinBase {
    open var objectOptionalVar: overrides.Parent? {
        get {
            return { switch Parent_objectOptionalVar_get(self.__externalRCRef()) { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    open var objectVar: overrides.Parent {
        get {
            return overrides.Parent.__createClassWrapper(externalRCRef: Parent_objectVar_get(self.__externalRCRef()))
        }
    }
    open var primitiveTypeVar: Swift.Int32 {
        get {
            return Parent_primitiveTypeVar_get(self.__externalRCRef())
        }
    }
    open var subtypeObjectVar: overrides.Parent {
        get {
            return overrides.Parent.__createClassWrapper(externalRCRef: Parent_subtypeObjectVar_get(self.__externalRCRef()))
        }
    }
    open var subtypeOptionalObjectVar: overrides.Parent? {
        get {
            return { switch Parent_subtypeOptionalObjectVar_get(self.__externalRCRef()) { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }()
        }
    }
    open var subtypeOptionalPrimitiveVar: Swift.Int32? {
        get {
            return Parent_subtypeOptionalPrimitiveVar_get(self.__externalRCRef()).map { it in it.int32Value }
        }
    }
    public final var value: Swift.String {
        get {
            return Parent_value_get(self.__externalRCRef())
        }
    }
    public init(
        value: Swift.String
    ) {
        let __kt = __root___Parent_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Parent_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, value); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public static func ==(
        this: overrides.Parent,
        to: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.equals(to: to)
    }
    open func actuallyOverride(
        nullable: Swift.Int32,
        poly: overrides.Child,
        nullablePoly: overrides.Child
    ) -> Swift.Void {
        return { Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__(self.__externalRCRef(), nullable, poly.__externalRCRef(), nullablePoly.__externalRCRef()); return () }()
    }
    open func contains(
        element: Swift.Int32
    ) -> Swift.Bool {
        return Parent_contains__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), element)
    }
    open func equals(
        to: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), to.map { it in it.__externalRCRef() } ?? nil)
    }
    open func finalOverrideFunc() -> Swift.Void {
        return { Parent_finalOverrideFunc(self.__externalRCRef()); return () }()
    }
    open func finalOverrideHopFunc() -> Swift.Void {
        return { Parent_finalOverrideHopFunc(self.__externalRCRef()); return () }()
    }
    open func genericReturnTypeFunc() -> [overrides.Parent] {
        return Parent_genericReturnTypeFunc(self.__externalRCRef()) as! Swift.Array<overrides.Parent>
    }
    open func hopFunc() -> Swift.Void {
        return { Parent_hopFunc(self.__externalRCRef()); return () }()
    }
    open func nonoverride() -> Swift.Int32 {
        return Parent_nonoverride(self.__externalRCRef())
    }
    open func objectFunc(
        arg: overrides.Child
    ) -> overrides.Parent {
        return overrides.Parent.__createClassWrapper(externalRCRef: Parent_objectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open func objectOptionalFunc(
        arg: overrides.Child
    ) -> overrides.Parent? {
        return { switch Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()) { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }()
    }
    open func overrideChainFunc() -> Swift.Void {
        return { Parent_overrideChainFunc(self.__externalRCRef()); return () }()
    }
    open func primitiveTypeFunc(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
    open func subtypeObjectFunc(
        arg: overrides.Child
    ) -> overrides.Parent {
        return overrides.Parent.__createClassWrapper(externalRCRef: Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open func subtypeOptionalObjectFunc() -> overrides.Parent? {
        return { switch Parent_subtypeOptionalObjectFunc(self.__externalRCRef()) { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }()
    }
    open func subtypeOptionalPrimitiveFunc() -> Swift.Int32? {
        return Parent_subtypeOptionalPrimitiveFunc(self.__externalRCRef()).map { it in it.int32Value }
    }
    public static func ~=(
        this: overrides.Parent,
        element: Swift.Int32
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
@_cdecl("AbstractBase_abstractFun1__reverse_swift")
public func AbstractBase_abstractFun1__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.AbstractBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.abstractFun1()
    return { _result; return true }()
}

@_cdecl("AbstractBase_abstractFun2__reverse_swift")
public func AbstractBase_abstractFun2__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.AbstractBase.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.abstractFun2()
    return { _result; return true }()
}

@_cdecl("AbstractDerived2_abstractFun1__reverse_swift")
public func AbstractDerived2_abstractFun1__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.AbstractDerived2.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.abstractFun1()
    return { _result; return true }()
}

@_cdecl("Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent_____reverse_swift")
public func Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ nullable: Foundation.NSNumber?, _ poly: Swift.UnsafeMutableRawPointer, _ nullablePoly: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.actuallyOverride(nullable: nullable.map { it in it.int32Value }, poly: overrides.Parent.__createClassWrapper(externalRCRef: poly), nullablePoly: { switch nullablePoly { case nil: .none; case let res: overrides.Parent.__createClassWrapper(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("Child_contains__TypesOfArguments__Swift_Int32____reverse_swift")
public func Child_contains__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.Int32) -> Swift.Bool {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.contains(element: element)
    return _result
}

@_cdecl("Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
public func Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ to: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.equals(to: { switch to { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("Child_genericReturnTypeFunc__reverse_swift")
public func Child_genericReturnTypeFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Any {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Array<overrides.Child> = _self.genericReturnTypeFunc()
    return _result
}

@_cdecl("Child_nonoverride__reverse_swift")
public func Child_nonoverride__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Never = _self.nonoverride()
    return { _result }()
}

@_cdecl("Child_objectFunc__TypesOfArguments__overrides_Child____reverse_swift")
public func Child_objectFunc__TypesOfArguments__overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: overrides.Parent = _self.objectFunc(arg: overrides.Child.__createClassWrapper(externalRCRef: arg))
    return _result.__externalRCRef()
}

@_cdecl("Child_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift")
public func Child_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<overrides.Parent> = _self.objectOptionalFunc(arg: overrides.Child.__createClassWrapper(externalRCRef: arg))
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("Child_overrideChainFunc__reverse_swift")
public func Child_overrideChainFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.overrideChainFunc()
    return { _result; return true }()
}

@_cdecl("Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift")
public func Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.Int32) -> Swift.Int32 {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.primitiveTypeFunc(arg: arg)
    return _result
}

@_cdecl("Child_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift")
public func Child_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: overrides.Child = _self.subtypeObjectFunc(arg: overrides.Child.__createClassWrapper(externalRCRef: arg))
    return _result.__externalRCRef()
}

@_cdecl("Child_subtypeOptionalObjectFunc__reverse_swift")
public func Child_subtypeOptionalObjectFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: overrides.Child = _self.subtypeOptionalObjectFunc()
    return _result.__externalRCRef()
}

@_cdecl("Child_subtypeOptionalPrimitiveFunc__reverse_swift")
public func Child_subtypeOptionalPrimitiveFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = overrides.Child.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.subtypeOptionalPrimitiveFunc()
    return _result
}

@_cdecl("OpenDerived1_abstractFun1__reverse_swift")
public func OpenDerived1_abstractFun1__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.OpenDerived1.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.abstractFun1()
    return { _result; return true }()
}

@_cdecl("OpenDerived1_abstractFun2__reverse_swift")
public func OpenDerived1_abstractFun2__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.OpenDerived1.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.abstractFun2()
    return { _result; return true }()
}

@_cdecl("Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child____reverse_swift")
public func Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ nullable: Swift.Int32, _ poly: Swift.UnsafeMutableRawPointer, _ nullablePoly: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.actuallyOverride(nullable: nullable, poly: overrides.Child.__createClassWrapper(externalRCRef: poly), nullablePoly: overrides.Child.__createClassWrapper(externalRCRef: nullablePoly))
    return { _result; return true }()
}

@_cdecl("Parent_contains__TypesOfArguments__Swift_Int32____reverse_swift")
public func Parent_contains__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.Int32) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.contains(element: element)
    return _result
}

@_cdecl("Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
public func Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ to: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.equals(to: { switch to { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("Parent_finalOverrideFunc__reverse_swift")
public func Parent_finalOverrideFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.finalOverrideFunc()
    return { _result; return true }()
}

@_cdecl("Parent_finalOverrideHopFunc__reverse_swift")
public func Parent_finalOverrideHopFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.finalOverrideHopFunc()
    return { _result; return true }()
}

@_cdecl("Parent_genericReturnTypeFunc__reverse_swift")
public func Parent_genericReturnTypeFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Any {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Array<overrides.Parent> = _self.genericReturnTypeFunc()
    return _result
}

@_cdecl("Parent_hopFunc__reverse_swift")
public func Parent_hopFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.hopFunc()
    return { _result; return true }()
}

@_cdecl("Parent_nonoverride__reverse_swift")
public func Parent_nonoverride__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.nonoverride()
    return _result
}

@_cdecl("Parent_objectFunc__TypesOfArguments__overrides_Child____reverse_swift")
public func Parent_objectFunc__TypesOfArguments__overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: overrides.Parent = _self.objectFunc(arg: overrides.Child.__createClassWrapper(externalRCRef: arg))
    return _result.__externalRCRef()
}

@_cdecl("Parent_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift")
public func Parent_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<overrides.Parent> = _self.objectOptionalFunc(arg: overrides.Child.__createClassWrapper(externalRCRef: arg))
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("Parent_overrideChainFunc__reverse_swift")
public func Parent_overrideChainFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.overrideChainFunc()
    return { _result; return true }()
}

@_cdecl("Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift")
public func Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.Int32) -> Swift.Int32 {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.primitiveTypeFunc(arg: arg)
    return _result
}

@_cdecl("Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift")
public func Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: overrides.Parent = _self.subtypeObjectFunc(arg: overrides.Child.__createClassWrapper(externalRCRef: arg))
    return _result.__externalRCRef()
}

@_cdecl("Parent_subtypeOptionalObjectFunc__reverse_swift")
public func Parent_subtypeOptionalObjectFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<overrides.Parent> = _self.subtypeOptionalObjectFunc()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("Parent_subtypeOptionalPrimitiveFunc__reverse_swift")
public func Parent_subtypeOptionalPrimitiveFunc__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Foundation.NSNumber? {
    let _self = overrides.Parent.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<Swift.Int32> = _self.subtypeOptionalPrimitiveFunc()
    return _result.map { it in NSNumber(value: it) } ?? nil
}
