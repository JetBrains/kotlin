internal interface INode {
    val tag: Tag?
    fun toKotlin(): String?
}