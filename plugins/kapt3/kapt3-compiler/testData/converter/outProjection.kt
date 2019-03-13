import kotlin.reflect.KClass;

annotation class Anno(val baseClass: KClass<out Intf<*, *, *>>)

interface Intf<S, R, A>

abstract class Base<S, R, A> : Intf<S, R, A>

@Anno(baseClass = Base::class)
interface Impl