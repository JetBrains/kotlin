package kotlinApi

public open class KotlinClass(public var field: Int) {
    public var property: String = ""
    public var nullableProperty: String? = ""

    public open fun foo(mutableCollection: MutableCollection<String>, nullableCollection: Collection<Int>?): MutableList<Any> = arrayListOf()

    fun get(i: Int) = 0

    companion object {
        @JvmStatic
        public fun staticFun(p: Int): Int = p
        @JvmStatic
        public var staticVar: Int = 1
        @JvmStatic
        public var staticProperty: Int
            get() = 1
            set(value) {}

        public fun nullableStaticFun(p: Int?): Int? = p
        public var nullableStaticVar: Int? = 1

        const val CONST = 0
    }
}

public interface KotlinTrait {
    public fun nullableFun(): String?
    public fun notNullableFun(): String

    public fun nonAbstractFun(): Int = 1
}

public fun globalFunction(s: String): String = s
public fun nullableGlobalFunction(s: String?): String? = s
public fun <T> globalGenericFunction(t: T): T = t

public fun Int.extensionFunction(): String = toString()

public var globalValue1: Int = 1
public var globalValue2: Int
    get() = 0
    set(value) {}

public var String.extensionProperty: Int
  get() = 1
  set(value) {}

public object KotlinObject {
    @JvmStatic
    public fun foo(): Int = 1
    @JvmStatic
    public var property1: Int = 1
    public var property2: Int
        get() = 1
        set(value) {}
}

public open class KotlinClassWithProperties {
    public open var someVar1: String = ""
    public open var someVar2: String = ""
    public open var someVar3: String = ""
    public open var someVar4: String
        get() = ""
        set(value) {}
    public open val someVal: String = ""

    public open fun getSomething1() { return 1; }

    public open fun getSomething2() { return 1; }
    public open fun setSomething2(value: Int) { }

    public open fun getSomething3() { return 1; }
    public open fun setSomething3(value: Int) { }

    public open fun getSomething4() { return 1; }
    public open fun setSomething4(value: Int) { }
}

public abstract class KotlinClassAbstractProperty {
    abstract val isVisible: Boolean
}
