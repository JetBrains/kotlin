class My {
    fun run() {
        val foo = <caret>doThing()
        System.out.println(foo)
    }

    private fun doThing(): Int {
        val foo = 1 + 2
        return foo
    }
}