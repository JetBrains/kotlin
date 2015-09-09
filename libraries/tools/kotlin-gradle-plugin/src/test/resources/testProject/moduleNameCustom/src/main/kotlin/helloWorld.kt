package demo

public fun isModuleFileExists(): Boolean {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val moduleFile = "META-INF/myTestName.kotlin_module"
    val resourceAsStream = systemClassLoader.getResourceAsStream(moduleFile)
    return resourceAsStream != null
}

