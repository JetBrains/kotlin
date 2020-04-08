// "Replace '@JvmField' with 'const'" "true"
// WITH_RUNTIME
const val three = 3
<caret>@JvmField private val text = "${2 + three}"