class B(i: Int) {
    fun call(): Int {
        return 1
    }
}

class A : B(10) {

    override fun call(): Int {
        return super.call()
    }
}