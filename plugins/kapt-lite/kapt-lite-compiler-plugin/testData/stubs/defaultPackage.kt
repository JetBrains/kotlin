class ClassInDefaultPackage {
    fun foo() {}
}

fun funInDefaultPackage() {}

const val valInDefaultPackage: String = ""

fun String.extensionFunInDefaultPackage(): Boolean {
    return this.length > 0
}