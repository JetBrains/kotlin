abstract class A {
    abstract fun memberFunInA()
    abstract val memberValInA: Int

    inner class InnerInA
    class NestedInA
}

fun A.extensionFun(){}

val A.extensionVal: Int
    get() = 1

fun Any.anyExtensionFun(){}
fun String.wrongExtensionFun(){}

fun globalFun(p: Int) {}
val globalVal = 1

class C {
    fun memberFun(){}

    val memberVal = 1

    fun A.memberExtensionFun(){}

    fun foo() {
        fun localFun(){}

        val v = A::<caret>
    }

    companion object {
        fun companionObjectFun(){}

        fun A.companionExtension(){}
    }
}

// EXIST: { lookupString: "class", itemText: "class", attributes: "bold" }
// EXIST_JAVA_ONLY: { lookupString: "class.java", itemText: "class", tailText: ".java", attributes: "bold" }
// EXIST: { itemText: "memberFunInA", attributes: "bold" }
// EXIST: { itemText: "memberValInA", attributes: "bold" }
// EXIST: { itemText: "InnerInA", attributes: "bold" }
// EXIST: { itemText: "NestedInA", attributes: "" }
// EXIST: { itemText: "extensionFun", attributes: "bold" }
// EXIST: { itemText: "extensionVal", attributes: "bold" }
// EXIST: { itemText: "anyExtensionFun", attributes: "" }
// ABSENT: wrongExtensionFun
// ABSENT: globalFun
// ABSENT: globalVal
// ABSENT: memberFun
// ABSENT: memberVal
// ABSENT: memberExtensionFun
// ABSENT: localFun
// ABSENT: companionObjectFun
// ABSENT: companionExtension
