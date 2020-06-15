// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch:  public fun <T> MutableCollection<in ???>.addAll(elements: Array<out ???>): Boolean defined in kotlin.collections public fun <T> MutableCollection<in ???>.addAll(elements: Sequence<???>): Boolean defined in kotlin.collections public fun <T> MutableCollection<in String?>.addAll(elements: Iterable<String?>): Boolean defined in kotlin.collections
internal class A<T> {
    fun foo(nonMutableCollection: Collection<String?>?,
            mutableCollection: Collection<String?>,
            mutableSet: MutableSet<T?>,
            mutableMap: MutableMap<String?, T>) {
        mutableCollection.addAll(nonMutableCollection)
        mutableSet.add(mutableMap.remove("a"))
    }
}