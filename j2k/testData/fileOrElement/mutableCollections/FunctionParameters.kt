// ERROR: Type mismatch: inferred type is T? but T was expected
import java.util.*

internal class A<T> {
    fun foo(
            nonMutableCollection: Collection<String>,
            mutableCollection: MutableCollection<String>,
            mutableSet: MutableSet<T>,
            mutableMap: MutableMap<String, T>
    ) {
        mutableCollection.addAll(nonMutableCollection)
        mutableSet.add(mutableMap.remove("a"))
    }
}