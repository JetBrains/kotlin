// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK

package test

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

open class Base
open class GenericBase<T>
interface First
interface Second
interface GenericInterface<T>

// No generic signature: superclass (-1) and both interface indices must remain distinct.
class Plain : @Anno Base(), @SourceAnno First, @Anno @SourceAnno Second
class ExplicitAny : @Anno @SourceAnno Any()

class Generic<@Anno T, @SourceAnno U> : @SourceAnno GenericBase<@Anno T>(), @Anno GenericInterface<@SourceAnno U>

// Bounds have their own indices; an interface-only bound still starts at index 1.
class Bounds<@Anno @SourceAnno T, U> where
        T : @Anno Base, T : @SourceAnno First, T : @Anno @SourceAnno Second,
        U : @Anno First, U : @SourceAnno Second

// Type-variable bounds
class TVBounds<@Anno T, U, V> where
        U : @Anno T, U : @SourceAnno First,
        V : @Anno First, V : @SourceAnno T

data class Data(val value: Int) : @Anno Base(), @SourceAnno First
enum class Enum : @Anno First, @SourceAnno Second { ENTRY }
interface Derived : @Anno First, @SourceAnno Second
fun interface Functional : @Anno First {
    fun invoke()
}
abstract class Abstract : @Anno Base(), @SourceAnno First
sealed class Sealed : @Anno Base(), @SourceAnno First
object Singleton : @Anno Base(), @SourceAnno First
data object DataSingleton : @SourceAnno Base(), @Anno Second

class Outer<T> {
    class Nested : @Anno Base(), @SourceAnno First
    inner class Inner<@SourceAnno U : @Anno First> : @SourceAnno GenericBase<T>(), @Anno GenericInterface<U>
    companion object : @Anno Base(), @SourceAnno First
}

class NamedCompanion {
    companion object Named : @SourceAnno Base(), @Anno Second
}

// Annotation classes must not acquire an explicit java.lang.annotation.Annotation superinterface.
annotation class AnnotationClass
