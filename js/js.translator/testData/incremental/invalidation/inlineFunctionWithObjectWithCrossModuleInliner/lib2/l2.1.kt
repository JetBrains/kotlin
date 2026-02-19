inline fun foo(): DemoInterface {
    return object : DemoInterface {
        override fun doAnything(): Int {
            return 1
        }
    }
}
