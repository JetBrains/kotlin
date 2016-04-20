package a

class Construction(param: Int)
object FactoryObject {
    @JvmStatic fun buildObject(/*rename*/param: Int) = Construction(param)
}