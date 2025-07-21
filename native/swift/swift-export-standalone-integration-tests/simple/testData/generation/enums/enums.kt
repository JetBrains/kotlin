// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

enum class Enum(var i: Int, private val s: String) {
    a(1, "str"), b(5, "rts");

    fun print(): String = "$i - $s"
}

fun enumId(e: kotlin.Enum<*>): kotlin.Enum<*> = e

enum class EnumSimple {
    FIRST,
    SECOND,
    LAST;
}

enum class EnumWithMembers {
    NORTH,
    SOUTH;

    fun foo() = if (this == NORTH) "N" else "S"

    val isNorth get() = this == NORTH
}

enum class EnumWithAbstractMembers {
    YELLOW {
        override fun blue(): Int {
            return 0
        }

        override fun green(): Int {
            return 0
        }

        override val red: Int
            get() = 255
    },
    SKY {
        override fun green(): Int = 255

        override val red: Int
            get() = 0
    },
    MAGENTA {
        override fun green(): Int = 0

        override val red: Int
            get() = 255
    };

    abstract fun green(): Int

    abstract val red: Int

    open fun blue(): Int = 255

    fun ordinalSquare() = this.ordinal * this.ordinal
}

//fun ewamEntries() = EnumWithAbstractMembers.entries

fun ewamValues() = EnumWithAbstractMembers.values()

fun yellow() = EnumWithAbstractMembers.valueOf("YELLOW")
