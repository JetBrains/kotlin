package foo

trait WithNumber {
    var number: Int
}

class IncNumber(val inc: Int) {
    fun get(withNumber: WithNumber, property: PropertyMetadata): Int {
        return withNumber.number + inc;
    }
    fun set(withNumber: WithNumber, property: PropertyMetadata, value: Int) {
        withNumber.number = value;
    }
}

class A : WithNumber {
    override var number: Int = 5
    var nextNumber by IncNumber(3)
}

fun box(): String {
    if (A().nextNumber != 8) return "A().nextNumber != 8, it: ${A().nextNumber}"

    val a = A()
    a.nextNumber = 10;
    if (a.number != 10) return "a.number != 10, it: " + a.number
    if (a.nextNumber != 13) return "a.nextNumber != 13, it: " + a.nextNumber
    return "OK"
}
