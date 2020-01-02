class Foo {
    var state: Int? = null
        private set

    fun setState(state: Int) {
        //some comment 1
        this.state = state
        //some comment 2
        if (state == 2) println("2")
    }

}