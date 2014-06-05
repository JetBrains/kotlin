package kotlinApi

public open class KotlinClass {
    public var property: String = ""
}

public fun globalFunction(s: String): String = s
public fun globalGenericFunction<T>(t: T): T = t

public fun Int.extensionFunction(): String = toString()

public var globalValue1: Int = 1
public var globalValue2: Int
  get() = 0
  set(value) {}

public var String.extensionProperty: Int
  get() = 1
  set(value) {}