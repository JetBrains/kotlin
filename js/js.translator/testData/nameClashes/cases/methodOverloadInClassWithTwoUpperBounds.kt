package foo

trait TraitA
trait TraitB

public open abstract class Node<T : TraitA>() where T : TraitB {
    public abstract fun bar(arg: T): String
    public abstract fun bar(arg: TraitA): String
    public abstract fun bar(arg: TraitB): String
}

class ClassAB : TraitA, TraitB

public class MyNode : Node<ClassAB>() {
    override fun bar(arg: ClassAB): String = "MyNode.bar(ClassAB)"
    override fun bar(arg: TraitA): String = "MyNode.bar(TraitA)"
    override fun bar(arg: TraitB): String = "MyNode.bar(TraitB)"
}


fun box(): String {

    var node = MyNode()
    assertEquals("MyNode.bar(ClassAB)", node.bar(ClassAB()))
    assertEquals("MyNode.bar(TraitA)", node.bar(ClassAB(): TraitA))
    assertEquals("MyNode.bar(TraitB)", node.bar(ClassAB(): TraitB))

    return "OK"
}