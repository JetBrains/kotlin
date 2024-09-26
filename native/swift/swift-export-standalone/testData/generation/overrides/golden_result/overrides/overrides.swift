@_implementationOnly import KotlinBridges_overrides
import KotlinRuntime

open class Child : overrides.Parent {
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
    public override init() {
        let __kt = __root___Child_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Child_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public final override func finalOverrideFunc() -> Swift.Void {
        return Child_finalOverrideFunc(self.__externalRCRef())
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
public final class GrandChild : overrides.Child {
    public override init() {
        let __kt = __root___GrandChild_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
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
open class Parent : KotlinRuntime.KotlinBase {
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
            return Parent_subtypeOptionalPrimitiveVar_get(self.__externalRCRef())?.int32Value
        }
    }
    public override init() {
        let __kt = __root___Parent_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Parent_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    open func finalOverrideFunc() -> Swift.Void {
        return Parent_finalOverrideFunc(self.__externalRCRef())
    }
    open func finalOverrideHopFunc() -> Swift.Void {
        return Parent_finalOverrideHopFunc(self.__externalRCRef())
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
        return Parent_subtypeOptionalPrimitiveFunc(self.__externalRCRef())?.int32Value
    }
}
