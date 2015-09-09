package org.jetbrains

fun main(args : Array<String>) {
    System.out?.println(isModuleFileExists())
}

fun isModuleFileExists() : Boolean {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val moduleFile = "META-INF/test-moduleNameDefault.kotlin_module"
    val resourceAsStream = systemClassLoader.getResourceAsStream(moduleFile)
    return resourceAsStream != null
}