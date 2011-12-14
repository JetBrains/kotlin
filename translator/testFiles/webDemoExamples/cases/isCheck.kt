fun main(args : Array<String>) {
    System.out?.println(getStringLength("aaa"))
    System.out?.println(getStringLength(1))
}

fun getStringLength(obj : Any) : Int? {
    if (obj is String)
        return obj.length // no cast to String is needed
    return null
}