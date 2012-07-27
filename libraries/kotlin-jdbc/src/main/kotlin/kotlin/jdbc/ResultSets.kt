package kotlin.jdbc

import java.sql.*
import java.util.ArrayList
import java.util.Collection
import java.util.List
import java.util.Map

/**
 * Executes the specfied block with result set and then closes it
 */
public fun <R> ResultSet.use(block : (ResultSet) -> R) : R {
    try {
        return block(this)
    } finally {
        this.close()
    }
}

/**
* Creates an iterator through a [[ResultSet]]
*/
fun ResultSet.iterator() : Iterator<ResultSet> {
    val rs = this
    return object : Iterator<ResultSet>{
        public override val hasNext : Boolean
        get() = rs.next()

        public override fun next() : ResultSet = rs
    }
}

/**
 * Returns iterable that calls to the specified mapper function for each row
 */
fun <T> ResultSet.getMapped(fn : (ResultSet) -> T) : jet.Iterable<T> {
    val rs = this

    val iterator = object : Iterator<T>{
        public override val hasNext : Boolean
            get() = rs.next()

        public override fun next() : T = fn(rs)
    }

    return object : jet.Iterable<T> {
        public override fun iterator(): Iterator<T> = iterator
    }
}

/**
 * Returns array with column names
 */
fun ResultSet.getColumnNames() : jet.Array<String> {
    val meta = getMetaData().sure()
    return jet.Array<String>(meta.getColumnCount(), {meta.getColumnName(it + 1) ?: it.toString()})
}

/**
 * Return array filled with values from current row in the cursor. Values will have the same order as column's order
 * @columnNames you can specify column names to extract otherwise all columns will be extracted
 */
fun ResultSet.getValues(columnNames : jet.Array<String> = getColumnNames()) : jet.Array<Any?> {
    return jet.Array<Any?>(columnNames.size, {
        this[columnNames[it]]
    })
}

/**
 * Return map filled with values from current row in the cursor. Uses column names as keys for result map.
 * @param columnNames you can specify column names to extract otherwise all columns will be extracted
 */
fun ResultSet.getValuesAsMap(columnNames : jet.Array<String> = getColumnNames()) : java.util.Map<String, Any?> {
    val result = java.util.HashMap<String, Any?>(columnNames.size)

    columnNames.forEach {
        result[it] = this[it]
    }

    return result
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

private fun ResultSet.ensureHasRow() : ResultSet {
    if (!next()) {
        throw IllegalStateException("There are no rows left in cursor")
    }
    return this
}

/**
 * Returns int value from the cursor at first column. May be useful to get result of count(*)
 */
fun ResultSet.singleInt() : Int = ensureHasRow().getInt(1)

/**
 * Returns long value from the cursor at first column.
 */
fun ResultSet.singleLong() : Long = ensureHasRow().getLong(1)

/**
 * Returns double value from the cursor at first column.
 */
fun ResultSet.singleDouble() : Double = ensureHasRow().getDouble(1)

