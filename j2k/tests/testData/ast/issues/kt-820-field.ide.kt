package demo

class Container() {
    var myInt: Int = 1
}
class One() {
    class object {
        var myContainer: Container = Container()
    }
}
class Test() {
    var b: Byte = One.myContainer.myInt.toByte()
}