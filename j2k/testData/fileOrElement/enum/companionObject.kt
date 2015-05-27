public enum class TestEnum {
    A,
    B;


    companion object {

        public fun parse(): TestEnum {
            return A
        }
    }
}

class Go {
    fun fn() {
        val x = TestEnum.parse()
    }
}
