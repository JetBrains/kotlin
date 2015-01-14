public class Test(count: Int) {
    public var count: Int = 0
        private set

    {
        this.count = count
    }

    public fun inc() {
        count++
    }
}
