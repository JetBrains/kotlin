object LinuxX64Main {
    fun invoke() = apply {
        NativeMain.invoke()
        CommonMain.invoke()
    }
}