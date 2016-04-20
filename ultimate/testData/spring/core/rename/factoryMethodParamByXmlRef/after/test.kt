package a

class Construction(param: Int)
object FactoryObject {
    @JvmStatic fun buildObject(param2: Int) = Construction(param2)
}