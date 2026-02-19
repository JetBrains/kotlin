package org.jetbrains.kotlin.jvm.abi

import java.io.File
import java.net.URLClassLoader

abstract class AbstractCompileAgainstJvmAbiTest : BaseJvmAbiTest() {
    fun doTest(path: String) {
        val testDir = File(path)
        val lib = Compilation(testDir, "lib").also { make(it) }
        val app = Compilation(testDir, "app", dependencies = listOf(lib)).also { make(it) }
        runApp(app)
    }

    private fun runApp(compilation: Compilation) {
        val runtimeDeps = compilation.dependencies.map { dep ->
            check(dep.destinationDir.exists()) { "Dependency '${dep.name}' of '${compilation.name}' was not built" }
            dep.destinationDir
        }

        val runtimeClasspath = listOf(compilation.destinationDir) + runtimeDeps + kotlinJvmStdlib
        val urls = runtimeClasspath.map { it.toURI().toURL() }.toTypedArray()
        val classloader = URLClassLoader(urls)
        val appClass = classloader.loadClass("app.AppKt")
        val runAppMethod = appClass.getMethod("runAppAndReturnOk")

        assertEquals("OK", runAppMethod.invoke(null))
    }
}