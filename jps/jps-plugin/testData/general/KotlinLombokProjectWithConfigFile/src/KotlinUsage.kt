fun main() {
    val obj = SomePojo()
    obj.name = "test"
    obj.age = 12
    val v = obj.isHuman
    obj.isHuman = !v
    println(obj)
}
