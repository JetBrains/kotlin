idePluginDependency {
    apply<JavaPlugin>()

    publish()

    val jar: Jar by tasks

    jar.apply {
        listOf(
            "compiler/testData/asJava/lightClasses",
            "compiler/testData/asJava/script",
            "compiler/testData/asJava/ultraLightClasses",
            "compiler/testData/asJava/ultraLightFacades",
            "compiler/testData/asJava/ultraLightScripts",
            "compiler/testData/loadJava/compiledKotlin",
            "compiler/fir/analysis-tests/testData/resolve",
            "compiler/fir/analysis-tests/testData/resolveWithStdlib",
            "compiler/testData/diagnostics/tests",
            "compiler/testData/diagnostics/helpers",
            "compiler/tests-spec/testData",
            "compiler/testData/diagnostics/testsWithStdLib",
            "compiler/fir/raw-fir/psi2fir/testData",
        ).forEach {
            from(rootDir.resolve(it)) {
                into(it)
            }
        }
    }
}
