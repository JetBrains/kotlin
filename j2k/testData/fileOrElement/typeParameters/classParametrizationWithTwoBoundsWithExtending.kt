internal interface INode
internal open class A

internal class C<T : INode> : A() where T : Comparable<T>