class Test(count: Int) {
    var count: Int
        private set

    fun inc() {
        this.count++
    }

    init {
        this.count = count
    }
}