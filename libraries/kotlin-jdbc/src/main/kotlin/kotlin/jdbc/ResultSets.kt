package kotlin.jdbc

import java.sql.*
import java.util.ArrayList
import java.util.Collection
import java.util.List

/**
* Creates an iterator through a [[ResultSet]]
*/
fun ResultSet.iterator() : Iterator<ResultSet> {
    val rs = this
    return object : Iterator<ResultSet>{
        override val hasNext : Boolean
        get() = rs.next()

        override fun next() : ResultSet = rs
    }
}

/**
 * Returns the value at the given column index (starting at 1)
 */
fun ResultSet.get(columnId: Int): Any? = this.getObject(columnId)

/**
 * Returns the value at the given column name
 */
fun ResultSet.get(columnName: String): Any? = this.getObject(columnName)

/**
 * Maps the collection of rows to some value
 */
fun <T> ResultSet.map(fn: (ResultSet) -> T) : List<T> {
    val answer = ArrayList<T>()
    mapTo(answer, fn)
    return answer
}

/**
 * Maps the collection of rows to some value and adds it to the result collection
 */
fun <T> ResultSet.mapTo(result: Collection<T>, fn: (ResultSet) -> T) : Collection<T> {
    for (row in this) {
        val element = fn(row)
        result.add(element)
    }
    return result
}
