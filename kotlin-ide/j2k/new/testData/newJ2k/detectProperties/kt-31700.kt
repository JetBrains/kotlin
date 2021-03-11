//Interface
interface Parent {
    val x: Unit
} //Subclass

class Child : Parent {
    override val x: Unit
        get() {}
}