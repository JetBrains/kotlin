open class Foo<Base>(base: Base)

class Bar<Base>(baseA: Base, baseB: Base) : Foo<Base>(baseA) {
    fun method(base1: Base, base2: Base, base3: Base): Base = base1
    fun Base.methodExtension() = Unit
    val property: Base? = null
    val Base.propertyExtension: Base get() = this
}

class TType<T>
class BooleanType<Boolean>
class NSObjectType<NSObject>
class MutableSetType<MutableSet>
class NSCopyingType<NSCopying>
class NSErrorType<NSError>
class doubleType<double>