class companion_object_2 {
    companion object {
        var fieldCompanion: Int = 5790

        fun create(): Int {
            return 5
        }
    }
}

fun companion_object_2_test(v: Int): Int {
    companion_object_2.fieldCompanion = 239 + v
    return companion_object_2.fieldCompanion
}