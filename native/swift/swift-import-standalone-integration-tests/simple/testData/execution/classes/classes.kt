fun main() {
    val counter = Counter()
    counter.increment()
    counter.increment()
    counter.printCount()

    val other = Counter()
    other.printCount()
    counter.printCount()

    val pureSwift = PureSwift()
    pureSwift.hello()
}
