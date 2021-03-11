fun use() {
    val myUser = <caret>object : User<String> {
        override fun call(arg: String) {}
    }
}