class Test(count: Int) {
    var count: Int
        private set

    init {
        this.count = count
    }

    fun inc() {
        count++
    }
}
