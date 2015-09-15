enum class TestEnum {
    A,
    B;


    companion object {

        fun parse(): TestEnum {
            return A
        }
    }
}

internal class Go {
    internal fun fn() {
        val x = TestEnum.parse()
    }
}
