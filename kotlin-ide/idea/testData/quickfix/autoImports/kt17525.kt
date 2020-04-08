// "class org.jetbrains.kotlin.idea.quickfix.ImportConstructorReferenceFix" "false"
// ERROR: Unresolved reference: ItsInner


class WithInner {
    inner class ItsInner
}

fun referInner(p: WithInner.ItsInner) {
    val v = p::ItsInner<caret>()
}