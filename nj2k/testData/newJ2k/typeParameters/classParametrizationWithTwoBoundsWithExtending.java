interface INode {}
class A {}

final class C<T extends INode & Comparable<? super T>> extends A {}