@_implementationOnly import KotlinBridges_overrides
import KotlinRuntime
import KotlinRuntimeSupport

open class AbstractBase: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    open var abstractVal: Swift.Int32 {
        get {
            return AbstractBase_abstractVal_get(self.__externalRCRef())
        }
    }
    package override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    package init(
        x: Swift.Int32
    ) {
        fatalError()
    }
    open func abstractFun1() -> Swift.Void {
        return AbstractBase_abstractFun1(self.__externalRCRef())
    }
    open func abstractFun2() -> Swift.Void {
        return AbstractBase_abstractFun2(self.__externalRCRef())
    }
}
open class AbstractDerived2: overrides.OpenDerived1 {
    package override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    package override init(
        x: Swift.Int32
    ) {
        fatalError()
    }
    open override func abstractFun1() -> Swift.Void {
        return AbstractDerived2_abstractFun1(self.__externalRCRef())
    }
}
open class Child: overrides.Parent {
    open override var objectOptionalVar: overrides.Parent? {
        get {
            return switch Child_objectOptionalVar_get(self.__externalRCRef()) { case 0: .none; case let res: overrides.Parent(__externalRCRef: res); }
        }
    }
    open override var objectVar: overrides.Parent {
        get {
            return overrides.Parent(__externalRCRef: Child_objectVar_get(self.__externalRCRef()))
        }
    }
    open override var primitiveTypeVar: Swift.Int32 {
        get {
            return Child_primitiveTypeVar_get(self.__externalRCRef())
        }
    }
    open override var subtypeObjectVar: overrides.Child {
        get {
            return overrides.Child(__externalRCRef: Child_subtypeObjectVar_get(self.__externalRCRef()))
        }
    }
    open override var subtypeOptionalObjectVar: overrides.Child {
        get {
            return overrides.Child(__externalRCRef: Child_subtypeOptionalObjectVar_get(self.__externalRCRef()))
        }
    }
    open override var subtypeOptionalPrimitiveVar: Swift.Int32 {
        get {
            return Child_subtypeOptionalPrimitiveVar_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, value)
    }
    public override init(
        value: Swift.String
    ) {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(__kt, value)
    }
    public init(
        nullable: Swift.Int32,
        poly: overrides.Parent,
        nullablePoly: overrides.Parent
    ) {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_overrides_Parent_overrides_Parent__(__kt, nullable, poly.__externalRCRef(), nullablePoly.__externalRCRef())
    }
    open override func actuallyOverride(
        nullable: Swift.Int32?,
        poly: overrides.Parent,
        nullablePoly: overrides.Parent?
    ) -> Swift.Void {
        return Child_actuallyOverride__TypesOfArguments__Swift_Int32_opt__overrides_Parent_overrides_Parent_opt___(self.__externalRCRef(), nullable.map { it in NSNumber(value: it) } ?? nil, poly.__externalRCRef(), nullablePoly.map { it in it.__externalRCRef() } ?? 0)
    }
    public final override func finalOverrideFunc() -> Swift.Void {
        return Child_finalOverrideFunc(self.__externalRCRef())
    }
    open func genericReturnTypeFunc() -> [overrides.Child] {
        return Child_genericReturnTypeFunc(self.__externalRCRef()) as! Swift.Array<overrides.Child>
    }
    open func nonoverride() -> Swift.Never {
        return Child_nonoverride(self.__externalRCRef())
    }
    open override func objectFunc(
        arg: overrides.Child
    ) -> overrides.Parent {
        return overrides.Parent(__externalRCRef: Child_objectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open override func objectOptionalFunc(
        arg: overrides.Child
    ) -> overrides.Parent? {
        return switch Child_objectOptionalFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()) { case 0: .none; case let res: overrides.Parent(__externalRCRef: res); }
    }
    open override func overrideChainFunc() -> Swift.Void {
        return Child_overrideChainFunc(self.__externalRCRef())
    }
    open override func primitiveTypeFunc(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
    open override func subtypeObjectFunc(
        arg: overrides.Child
    ) -> overrides.Child {
        return overrides.Child(__externalRCRef: Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open override func subtypeOptionalObjectFunc() -> overrides.Child {
        return overrides.Child(__externalRCRef: Child_subtypeOptionalObjectFunc(self.__externalRCRef()))
    }
    open override func subtypeOptionalPrimitiveFunc() -> Swift.Int32 {
        return Child_subtypeOptionalPrimitiveFunc(self.__externalRCRef())
    }
}
public final class GrandChild: overrides.Child {
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public override init(
        value: Swift.Int32
    ) {
        let __kt = __root___GrandChild_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, value)
    }
    public override func finalOverrideHopFunc() -> Swift.Void {
        return GrandChild_finalOverrideHopFunc(self.__externalRCRef())
    }
    public override func hopFunc() -> Swift.Void {
        return GrandChild_hopFunc(self.__externalRCRef())
    }
    public override func overrideChainFunc() -> Swift.Void {
        return GrandChild_overrideChainFunc(self.__externalRCRef())
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
        super.init(__externalRCRef: __kt)
        __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public override init(
        x: Swift.Int32
    ) {
        let __kt = __root___OpenDerived1_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, x)
    }
    open override func abstractFun1() -> Swift.Void {
        return OpenDerived1_abstractFun1(self.__externalRCRef())
    }
    open override func abstractFun2() -> Swift.Void {
        return OpenDerived1_abstractFun2(self.__externalRCRef())
    }
}
open class Parent: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    open var objectOptionalVar: overrides.Parent? {
        get {
            return switch Parent_objectOptionalVar_get(self.__externalRCRef()) { case 0: .none; case let res: overrides.Parent(__externalRCRef: res); }
        }
    }
    open var objectVar: overrides.Parent {
        get {
            return overrides.Parent(__externalRCRef: Parent_objectVar_get(self.__externalRCRef()))
        }
    }
    open var primitiveTypeVar: Swift.Int32 {
        get {
            return Parent_primitiveTypeVar_get(self.__externalRCRef())
        }
    }
    open var subtypeObjectVar: overrides.Parent {
        get {
            return overrides.Parent(__externalRCRef: Parent_subtypeObjectVar_get(self.__externalRCRef()))
        }
    }
    open var subtypeOptionalObjectVar: overrides.Parent? {
        get {
            return switch Parent_subtypeOptionalObjectVar_get(self.__externalRCRef()) { case 0: .none; case let res: overrides.Parent(__externalRCRef: res); }
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
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        value: Swift.String
    ) {
        let __kt = __root___Parent_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Parent_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(__kt, value)
    }
    open func actuallyOverride(
        nullable: Swift.Int32,
        poly: overrides.Child,
        nullablePoly: overrides.Child
    ) -> Swift.Void {
        return Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__(self.__externalRCRef(), nullable, poly.__externalRCRef(), nullablePoly.__externalRCRef())
    }
    open func finalOverrideFunc() -> Swift.Void {
        return Parent_finalOverrideFunc(self.__externalRCRef())
    }
    open func finalOverrideHopFunc() -> Swift.Void {
        return Parent_finalOverrideHopFunc(self.__externalRCRef())
    }
    open func genericReturnTypeFunc() -> [overrides.Parent] {
        return Parent_genericReturnTypeFunc(self.__externalRCRef()) as! Swift.Array<overrides.Parent>
    }
    open func hopFunc() -> Swift.Void {
        return Parent_hopFunc(self.__externalRCRef())
    }
    open func nonoverride() -> Swift.Int32 {
        return Parent_nonoverride(self.__externalRCRef())
    }
    open func objectFunc(
        arg: overrides.Child
    ) -> overrides.Parent {
        return overrides.Parent(__externalRCRef: Parent_objectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open func objectOptionalFunc(
        arg: overrides.Child
    ) -> overrides.Parent? {
        return switch Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()) { case 0: .none; case let res: overrides.Parent(__externalRCRef: res); }
    }
    open func overrideChainFunc() -> Swift.Void {
        return Parent_overrideChainFunc(self.__externalRCRef())
    }
    open func primitiveTypeFunc(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
    open func subtypeObjectFunc(
        arg: overrides.Child
    ) -> overrides.Parent {
        return overrides.Parent(__externalRCRef: Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self.__externalRCRef(), arg.__externalRCRef()))
    }
    open func subtypeOptionalObjectFunc() -> overrides.Parent? {
        return switch Parent_subtypeOptionalObjectFunc(self.__externalRCRef()) { case 0: .none; case let res: overrides.Parent(__externalRCRef: res); }
    }
    open func subtypeOptionalPrimitiveFunc() -> Swift.Int32? {
        return Parent_subtypeOptionalPrimitiveFunc(self.__externalRCRef()).map { it in it.int32Value }
    }
}
