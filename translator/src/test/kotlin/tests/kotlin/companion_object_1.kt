class companion_object {
    companion object Factory {
        val fieldCompanion: Int = 5790

        fun create(): Int {
            return 5
        }
    }
}


fun companion_object_1(): Int {
    return companion_object.create()
}