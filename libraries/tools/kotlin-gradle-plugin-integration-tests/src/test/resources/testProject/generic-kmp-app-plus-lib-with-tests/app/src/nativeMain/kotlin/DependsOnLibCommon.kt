class AppNativeUnusedChildClass(length: Int, builder: String) : LibCommonClassForAppPlatform(length, builder) {
    fun price() = length * builder.length * 300
}
