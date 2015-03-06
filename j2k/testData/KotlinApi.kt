package kotlinApi

public open class KotlinClass(public var field: Int) {
    public var property: String = ""
    public var nullableProperty: String? = ""

    public open fun foo(mutableCollection: MutableCollection<String>, nullableCollection: Collection<Int>?): MutableList<Any> = arrayListOf()

    default object {
        public fun staticFun(p: Int): Int = p
        public var staticVar: Int = 1
        public var staticProperty: Int
            get() = 1
            set(value) {}

        public fun nullableStaticFun(p: Int?): Int? = p
        public var nullableStaticVar: Int? = 1
    }
}

public trait KotlinTrait {
    public fun nullableFun(): String?
    public fun notNullableFun(): String

    public fun nonAbstractFun(): Int = 1
}

public fun globalFunction(s: String): String = s
public fun nullableGlobalFunction(s: String?): String? = s
public fun globalGenericFunction<T>(t: T): T = t

public fun Int.extensionFunction(): String = toString()

public var globalValue1: Int = 1
public var globalValue2: Int
    get() = 0
    set(value) {}

public var String.extensionProperty: Int
  get() = 1
  set(value) {}

public object KotlinObject {
    public fun foo(): Int = 1
    public var property1: Int = 1
    public var property2: Int
        get() = 1
        set(value) {}
}