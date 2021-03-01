package org.jetbrains.kotlin

interface CompilerRunner {
    fun runCompiler(filesToCompile: List<String>, output: String, moreArgs: List<String>)
}

// This class knows how to run the compiler for multi-module external tests.
//
// In addition it knows how to deal with multi-versioned modules.
// The versioned modules are used for binary compatibility testing.
// For such modules we produce two klibs:
//      `version1/foo.klib` and `version2/foo.klib`.
// For un-versioned modules we still produce just `bar.klib`
// We link all klibs against `version1/foo.klib` and `bar.klib`
// We link the final executable against `version2/foo.klib` and `bar.klib`.

class MultiModuleCompilerInvocations(
    val konanTest: CompilerRunner,
    val outputDirectory: String,
    val executablePath: String,
    val modules: Map<String, TestModule>,
    val flags: List<String>
) {
    val libs = HashSet<String>()

    fun moduleToKlibName(moduleName: String, version: Int): String {
        val module = modules[moduleName] ?: error("Could not find module $moduleName")

        return if (module.hasVersions) {
            val versionSuffix = "_version$version"
            "$executablePath$versionSuffix/$moduleName.klib"
        } else {
            "$executablePath.$moduleName.klib"
        }
    }

    private fun produceLibrary(module: TestModule, moduleVersion: Int) {
        val klibModulePath = moduleToKlibName(module.name, moduleVersion)
        libs.addAll(module.dependencies)

        // When producing klibs link them against version 1 of multiversioned klibs.
        val dependencyVersion = 1

        val klibs = module.dependencies.flatMap { listOf("-l", moduleToKlibName(it, dependencyVersion)) }
        val repos = listOf("-r", "${executablePath}_version$dependencyVersion", "-r", outputDirectory)

        val friends = module.friends.flatMap {
                listOf("-friend-modules", "$executablePath.$it.klib")
        }
        konanTest.runCompiler(module.versionFiles(moduleVersion).map { it.path },
            klibModulePath, flags + listOf("-p", "library") + repos + klibs + friends)
    }

    fun produceLibrary(module: TestModule) {
        if (!module.hasVersions) {
            produceLibrary(module, 1)
        } else {
            produceLibrary(module, 1)
            produceLibrary(module, 2)
        }
    }

    fun produceProgram(compileList: List<TestFile>) {
        val compileMain = compileList.filter {
            it.module.isDefaultModule() || it.module === TestModule.support
        }
        compileMain.forEach { f ->
            libs.addAll(f.module.dependencies)
        }

        // When producing klibs link them against version 2 of multiversioned klibs.
        val dependencyVersion = 2

        val friends = compileMain.flatMap { it.module.friends }.toSet()
        val repos = listOf("-r", "${executablePath}_version$dependencyVersion", "-r", outputDirectory)

        if (compileMain.isNotEmpty()) {
            konanTest.runCompiler(compileMain.map { it.path }, executablePath, flags + repos +
                    libs.flatMap { listOf("-l", moduleToKlibName(it, dependencyVersion)) } +
                    friends.flatMap { listOf("-friend-modules", "${executablePath}.${it}.klib") }
            )
        }
    }
}
