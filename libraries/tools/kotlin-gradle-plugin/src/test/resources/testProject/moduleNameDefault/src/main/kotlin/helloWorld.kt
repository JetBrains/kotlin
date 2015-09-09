package demo


public fun isModuleFileExists(): Boolean {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val moduleFile = "META-INF/moduleNameDefault-compileKotlin.kotlin_module"
    val resourceAsStream = systemClassLoader.getResourceAsStream(moduleFile)
    return resourceAsStream != null
}

