// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: a
// ERROR: Unsupported [literal prefixes and suffixes]
open class A(a: () -> Unit)

class B: A({ "1"<caret> })