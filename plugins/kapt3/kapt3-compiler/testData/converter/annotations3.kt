// WITH_STDLIB

interface Parceler<T>

@Retention(AnnotationRetention.SOURCE)
@Repeatable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class TypeParceler<T, P: Parceler<in T>>

@TypeParceler<B, BParceler>
@TypeParceler<C, CParceler>
class Test

class B
class C

object BParceler : Parceler<B>
object CParceler : Parceler<C>