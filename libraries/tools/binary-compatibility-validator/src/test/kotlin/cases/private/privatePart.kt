package cases.private

// properties
private val privateVal: Any? = 1
private var privateVar: Any? = 1

// constants

private const val privateConst: Int = 4

// fun

private fun privateFun(a: Any?) = privateConst

// access
private class PrivateClassInPart {
    internal fun accessUsage() {
        privateFun(privateVal)
        privateFun(privateVar)
        privateFun(privateConst)
    }

}