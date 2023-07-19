// !LANGUAGE: +EnumEntries
enum class Enum1 {
    BLACK, WHITE
}

annotation class Anno1(val value: String)

enum class Enum2(@Anno1("first") val col: String, @Anno1("second") val col2: Int) {
    RED("red", 1), WHITE("white", 2);
    fun color() = col

    private fun privateEnumFun() {}
    public fun publicEnumFun() {}
}

enum class Nested1 {
    WHITE {
        enum class Nested2 {
            BLACK {
                enum class Nested3 { RED }
            }
        }
    };
}

interface I {
    enum class Nested { WHITE }
}
