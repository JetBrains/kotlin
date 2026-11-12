annotation class Ann

class Editor {
    companion object {
        @Ann
        private const val REQUEST_PREVIEWER = 0

        @Suppress("unused")
        private const val REQUEST_CARD_BROWSER_APPEARANCE = 1

        @Ann
        val zeta: Int = 2

        @Ann
        val alpha: Int = 3
    }
}
