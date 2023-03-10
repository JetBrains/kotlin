// IGNORE K2

interface A
interface B
class Inv<T>(e: T)

fun <S> intersection(x: Inv<in S>, y: Inv<in S>): S = TODO()

fun <K> use(k: K, f: K.(K) -> K) {}
fun <K> useNested(k: K, f: Inv<K>.(Inv<K>) -> Inv<K>) {}

fun <T> createDelegate(f: () -> T): Delegate<T> = Delegate()

class Delegate<T> {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T = TODO()
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {}
}

fun test(a: Inv<A>, b: Inv<B>) {
    val intersectionType = intersection(a, b)

    use(intersectionType) { intersectionType }
    useNested(intersectionType) { Inv(intersectionType) }

    var d by createDelegate { intersectionType }
}
