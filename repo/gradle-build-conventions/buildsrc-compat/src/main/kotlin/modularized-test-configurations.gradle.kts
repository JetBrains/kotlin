@file:Suppress("UnstableApiUsage")

fun MutableList<String>.addModularizedTestArgs(prefix: String, path: String, additionalParameters: Map<String, String>, benchFilter: String?) {
    add("-${prefix}fir.bench.prefix=$path")
    add("-${prefix}fir.bench.jps.dir=$path/test-project-model-dump")
    add("-${prefix}fir.bench.passes=1")
    add("-${prefix}fir.bench.dump=false")
    for ((name, value) in additionalParameters) {
        add("-$prefix$name=$value")
    }
    if (benchFilter != null) {
        add("-${prefix}fir.bench.filter=$benchFilter")
    }
}

fun generateArgsForGradleConfiguration(path: String, additionalParameters: Map<String, String>, benchFilter: String?): String {
    val args = mutableListOf<String>()
    args.addModularizedTestArgs(prefix = "P", path = path, additionalParameters = additionalParameters, benchFilter = benchFilter)
    return args.joinToString(" ")
}

fun generateXmlContentForGradleConfiguration(name: String, testClassName: String, vmParameters: String): String {
    return """
        <component name="ProjectRunConfigurationManager">
          <configuration default="false" name="$name" type="GradleRunConfiguration" factoryName="Gradle" folderName="Modularized tests">
            <ExternalSystemSettings>
              <option name="executionName" />
              <option name="externalProjectPath" value="${'$'}PROJECT_DIR${'$'}" />
              <option name="externalSystemIdString" value="GRADLE" />
              <option name="scriptParameters" value="--tests &quot;org.jetbrains.kotlin.fir.${testClassName}&quot; ${vmParameters}" />
              <option name="taskDescriptions">
                <list />
              </option>
              <option name="taskNames">
                <list>
                  <option value=":compiler:fir:modularized-tests:test" />
                </list>
              </option>
              <option name="vmOptions" value="" />
            </ExternalSystemSettings>
            <GradleScriptDebugEnabled>true</GradleScriptDebugEnabled>
            <method v="2" />
          </configuration>
        </component>
    """.trimIndent()
}

fun String.convertNameToRunConfigurationFile(prefix: String = ""): File {
    val fileName = prefix + replace("""[ -.\[\]]""".toRegex(), "_") + ".xml"
    return rootDir.resolve(".idea/runConfigurations/${fileName}")
}

fun generateGradleConfiguration(name: String, testClassName: String, path: String, additionalParameters: Map<String, String>, benchFilter: String?) {
    val vmParameters = generateArgsForGradleConfiguration(path, additionalParameters, benchFilter)
    val content = generateXmlContentForGradleConfiguration(
        name = name,
        testClassName = testClassName,
        vmParameters = vmParameters
    )
    name.convertNameToRunConfigurationFile().writeText(content)
}

data class Configuration(val path: String, val name: String, val additionalParameters: Map<String, String> = emptyMap()) {
    companion object {
        operator fun invoke(path: String?, name: String, additionalParameters: Map<String, String> = emptyMap()): Configuration? {
            return path?.let { Configuration(it, name, additionalParameters) }
        }
    }
}

val testDataPathList = listOfNotNull(
    Configuration(kotlinBuildProperties.pathToKotlinModularizedTestData, "Kotlin"),
    Configuration(kotlinBuildProperties.pathToIntellijModularizedTestData, "IntelliJ"),
    Configuration(kotlinBuildProperties.pathToYoutrackModularizedTestData, "YouTrack"),
    Configuration(kotlinBuildProperties.pathToSpaceModularizedTestData, "Space")
)

val generateMT = kotlinBuildProperties.generateModularizedConfigurations
val generateFP = kotlinBuildProperties.generateFullPipelineConfigurations

for ((path, projectName, additionalParameters) in testDataPathList) {
    rootProject.afterEvaluate {
        val configurations = mutableListOf<Pair<String, String?>>(
            "Full $projectName" to null
        )

        for ((name, benchFilter) in configurations) {
            if (generateMT) {
                generateGradleConfiguration(
                    "[MT] $name",
                    "FirResolveModularizedTotalKotlinTest",
                    path,
                    additionalParameters,
                    benchFilter
                )
            }
            if (generateFP) {
                generateGradleConfiguration(
                    "[FP] $name",
                    "FullPipelineModularizedTest",
                    path,
                    additionalParameters,
                    benchFilter
                )
            }
        }
    }
}
