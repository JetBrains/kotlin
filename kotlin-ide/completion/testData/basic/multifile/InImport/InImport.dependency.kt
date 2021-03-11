package second

fun String.extensionFun(){}
val Int.extensionVal: Int get() = 1

fun topLevelFun1(p: (String, Int) -> Unit){}
fun topLevelFun2(p: () -> Unit){}
val topLevelVal: Int = 1

