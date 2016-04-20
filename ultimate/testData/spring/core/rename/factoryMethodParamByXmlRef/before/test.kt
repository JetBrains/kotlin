package a

class Construction(param: Int)
object FactoryObject {
    @JvmStatic fun buildObject(param: Int) = Construction(param)
}