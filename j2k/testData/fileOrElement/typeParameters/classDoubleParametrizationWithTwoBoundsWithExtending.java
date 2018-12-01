interface INode {}
interface Node {}
class A {}
final class CC <T extends INode & Comparable<? super T>, K extends Node & Collection<? extends K>> extends A {}