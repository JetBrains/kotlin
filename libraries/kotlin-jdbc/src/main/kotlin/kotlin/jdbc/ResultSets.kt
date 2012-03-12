package kotlin.jdbc

import java.sql. *

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

