class Test(count: Int) {
    var count: Int = 0
        private set

    init {
        this.count = count
    }

    fun inc() {
        count++
    }
}
