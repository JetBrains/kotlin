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
    fun fn() {
        val x = TestEnum.parse()
    }
}
