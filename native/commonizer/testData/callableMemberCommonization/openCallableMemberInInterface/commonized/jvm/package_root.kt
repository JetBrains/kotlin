actual interface Interface {
    actual fun openFun() = Unit
    fun openFunWithOtherParams(param: Double) = Unit
    fun openInJs_abstractInJvm()
}
