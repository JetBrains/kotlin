namespace std

import java.util.Collection
import java.util.ArrayList
import java.util.LinkedList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.TreeSet

/*
 * Extension functions on the standard Kotlin types to behave like the java.lang.* and java.util.* collections
 */

/*
Add iterated elements to given container
*/
fun <T,U: Collection<in T>> Iterator<T>.to(container: U) : U {
    while(hasNext)
        container.add(next())
    return container
}

/*
Add iterated elements to java.util.ArrayList
*/
inline fun <T> Iterator<T>.toArrayList() = to(ArrayList<T>())

/*
Add iterated elements to java.util.LinkedList
*/
inline fun <T> Iterator<T>.toLinkedList() = to(LinkedList<T>())

/*
Add iterated elements to java.util.HashSet
*/
inline fun <T> Iterator<T>.toHashSet() = to(HashSet<T>())

/*
Add iterated elements to java.util.LinkedHashSet
*/
inline fun <T> Iterator<T>.toLinkedHashSet() = to(LinkedHashSet<T>())

/*
Add iterated elements to java.util.TreeSet
*/
inline fun <T> Iterator<T>.toTreeSet() = to(TreeSet<T>())

