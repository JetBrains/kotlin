fun test(): Int {
    val a = ClassA()
    val obj = a.leakObject()
    return obj.getNumber() + obj.extraNumber + obj.getOtherNumber() + obj.otherExtraNumber
}
