tasks.withType<Test>().configureEach {
    doFirst {
        val inputFiles = inputs.files.files
        val inputPaths = inputFiles.map { it.absolutePath }

        // Store the declared inputs in a file for the agent to read
        val inputsFile = File("${project.rootDir}/build/testInputs.txt")
        inputsFile.parentFile.mkdirs()
        inputsFile.writeText(inputPaths.joinToString("\n"))

        // Configure JVM args to use our agent
        // TODO: Configured in other place
//        jvmArgs("-javaagent:${project.rootDir}/buildSrc/build/libs/file-access-monitor-agent.jar=${inputsFile.absolutePath}")
    }

    doLast {
        // Check for violations after the test completes
        val accessedFilesPath = File("${project.rootDir}/accessedFiles.txt")
        if (accessedFilesPath.exists()) {
            val accessedFiles = accessedFilesPath.readLines()
            val inputsFile = File("${project.rootDir}/build/testInputs.txt")
            val declaredInputs = inputsFile.readLines()

            val violations = accessedFiles.filter { accessedFile ->
                !declaredInputs.any { input -> accessedFile.startsWith(input) }
            }

            if (violations.isNotEmpty()) {
                println("WARNING: Test task '${name}' accessed files outside declared inputs:")
                violations.forEach { println("  - $it") }

                // Optionally fail the build if configured to do so
                if (project.hasProperty("strictInputs") && project.property("strictInputs").toString().toBoolean()) {
                    throw IllegalStateException("Tests accessed undeclared inputs")
                }
            } else {
                println("All accessed files were within declared inputs for task '${name}'")
            }
        } else {
            error("No 'accessedFiles.txt' file was found in the output directory. Did the test task complete successfully?")
        }
    }
}