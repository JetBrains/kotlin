// INTENTION_TEXT: "Add import for 'kotlin.LazyThreadSafetyMode.NONE'"
// WITH_RUNTIME

class A {
    val v1: Int by lazy(LazyThreadSafetyMode.NONE<caret>) { 1 }
    val v2: Int by lazy(LazyThreadSafetyMode.NONE) { 1 }
}