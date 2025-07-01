import gradle.GradlePluginVariant
import kotlin.io.path.pathString

configureGradlePluginDependency(
    withPublication = false
)

val dependenciesToCheck = listOf(
    project(":kotlin-gradle-plugin"),
    project(":kotlin-gradle-plugin-api"),
)

dependencies {
    dependenciesToCheck.forEach {
        "commonImplementation"(it)
    }
}

val testGradleCommon = tasks.register("testGradleCommon")
val dependencyPathsToCheck = dependenciesToCheck.map { it.projectDir.toPath() }

fun testGradleVariant(
    variantSourceSetName: String,
    dependencyPathsToCheck: List<java.nio.file.Path>,
    expectedCompilationClasspath: List<List<String>>,
) {
    expectedCompilationClasspath.isEmpty()
    val variantTest = tasks.register("testGradleCommon_${variantSourceSetName}") {
        val compilationClasspath = sourceSets.getByName(variantSourceSetName).compileClasspath
        doLast {
            val actualCompilationClasspath = compilationClasspath.files.map { it.toPath() }.mapNotNull { compilationPath ->
                val subprojectPath = dependencyPathsToCheck.firstOrNull { compilationPath.startsWith(it) }
                if (subprojectPath == null) { return@mapNotNull null }
                compilationPath.drop(subprojectPath.count() - 1).map { it.pathString }
            }
            if (actualCompilationClasspath != expectedCompilationClasspath) {
                error(
                    """
                    Expected compilation classpath: ${expectedCompilationClasspath},
                    Actual compilation classpath: ${actualCompilationClasspath}
                    """.trimIndent()
                )
            }
        }
    }
    testGradleCommon.configure {
        dependsOn(variantTest)
    }
}

testGradleVariant(
    variantSourceSetName = GradlePluginVariant.GRADLE_MIN.sourceSetName,
    dependencyPathsToCheck = dependencyPathsToCheck,
    expectedCompilationClasspath = listOf(
        listOf("kotlin-gradle-plugin", "build", "libs", "kotlin-gradle-plugin-${version}.jar"),
        listOf("kotlin-gradle-plugin-api", "build", "libs", "kotlin-gradle-plugin-api-${version}.jar"),
    )
)

testGradleVariant(
    variantSourceSetName = GradlePluginVariant.GRADLE_86.sourceSetName,
    dependencyPathsToCheck = dependencyPathsToCheck,
    expectedCompilationClasspath = listOf(
        listOf("kotlin-gradle-plugin", "build", "classes", "java", "common"),
        listOf("kotlin-gradle-plugin", "build", "classes", "kotlin", "common"),
        listOf("kotlin-gradle-plugin", "build", "classes", "java", "gradle86"),
        listOf("kotlin-gradle-plugin", "build", "classes", "kotlin", "gradle86"),
        listOf("kotlin-gradle-plugin-api", "build", "classes", "java", "common"),
        listOf("kotlin-gradle-plugin-api", "build", "classes", "kotlin", "common"),
        listOf("kotlin-gradle-plugin-api", "build", "classes", "java", "gradle86"),
        listOf("kotlin-gradle-plugin-api", "build", "classes", "kotlin", "gradle86"),
    )
)

testGradleVariant(
    variantSourceSetName = "common",
    dependencyPathsToCheck = dependencyPathsToCheck,
    expectedCompilationClasspath = listOf(
        listOf("kotlin-gradle-plugin", "build", "classes", "java", "common"),
        listOf("kotlin-gradle-plugin", "build", "classes", "kotlin", "common"),
        listOf("kotlin-gradle-plugin-api", "build", "classes", "java", "common"),
        listOf("kotlin-gradle-plugin-api", "build", "classes", "kotlin", "common"),
    )
)