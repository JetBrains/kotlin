fun test(): Int {
    val a = ClassA()
    val obj = a.leakedObject
    return obj.getNumber() + obj.extraNumber
}
