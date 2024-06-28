import kotlin.reflect.KClass

class A

typealias TA = KClass<A>

val ref1 = A::class
val ref2: KClass<A> = A::class
val ref3: TA = A::class
