// DUMP_LINE_MAPPINGS

interface JsonContainer<T> {
    val size: Int

    fun constructor(index: Int): T

    operator fun get(index: Int): T = constructor(index)

    fun length(): Int = size

    operator fun iterator(): Iterator<T> = List(size) { get(it) }.iterator()

    operator fun set(index: Int, value: T) {}
}

@JvmInline
value class CardTemplates(override val size: Int) : JsonContainer<String> {
    override fun constructor(index: Int): String = index.toString()
}

interface Named {
    fun name(): String

    val title: String get() = name()
}

@JvmInline
value class NameHolder(val named: Named) : Named by named
