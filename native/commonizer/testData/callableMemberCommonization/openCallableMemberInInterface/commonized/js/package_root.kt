actual interface Interface {
    actual fun openFun() = Unit
    fun openFunWithOtherParams(param: Int) = Unit
    fun openInJs_abstractInJvm() = Unit
}
