
class HashableObject(val value: Int) {
    override fun hashCode(): Int = value
    override fun equals(other: Any?): Boolean = (other as? HashableObject)?.value == value
    override fun toString(): String = "$value"
}

fun getHashableObject(value: Int): Any = HashableObject(value)
fun getHash(obj: Any): Int = obj.hashCode()
fun isEqual(lhs: Any, rhs: Any): Boolean = lhs == rhs