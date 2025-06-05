import com.github.gradle.node.NodeExtension
import com.github.gradle.node.exec.NodeExecConfiguration
import com.github.gradle.node.npm.exec.NpmExecRunner
import com.github.gradle.node.npm.task.NpxTask
import com.github.gradle.node.util.DefaultProjectApiHelper
import com.github.gradle.node.variant.VariantComputer
import groovy.json.JsonSlurper
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.ByteArrayOutputStream
import java.io.Serializable

description = "Simple Kotlin/Wasm devtools formatters"

plugins {
    base
    id("share-kotlin-wasm-custom-formatters")
    alias(libs.plugins.gradle.node)
}

node {
    version.set(nodejsVersion)
    download.set(true)
    nodeProjectDir.set(projectDir)
    npmInstallCommand.set("ci")
}

dependencies {
    implicitDependencies("org.nodejs:node:$nodejsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-arm64@tar.gz")
}

val cleanBuild by tasks.registering(Delete::class) {
    group = "build"

    delete = setOf("build")
}

val cleanNpm by tasks.registering(Delete::class) {
    group = "build"

    delete = setOf("node_modules")
}

val npmBuild by tasks.registering(NpxTask::class) {
    group = "build"

    dependsOn(tasks.npmInstall)
    dependsOn(addNodeModulesToNpmCache)

    val rollupConfigMjsFile = file("rollup.config.mjs")
    inputs.file(rollupConfigMjsFile)
        .withPropertyName("rollupConfigMjsFile")
        .normalizeLineEndings()
        .withPathSensitivity(RELATIVE)

    inputs.dir("src")
        .withPropertyName("src")
        .normalizeLineEndings()
        .withPathSensitivity(RELATIVE)

    command.set("rollup")
    workingDir.set(projectDir)
    args.set(listOf("-c", rollupConfigMjsFile.name, "--silent"))
    environment.set(mapOf("NODE_OPTIONS" to "--disable-warning=ExperimentalWarning"))

    outputs.file("build/out/custom-formatters.js")
}

tasks {
    npmInstall {
//        val nodeModulesDir = projectDir.resolve("node_modules")
//        outputs.upToDateWhen {
//            nodeModulesDir.isDirectory
//        }

        if (gradle.startParameter.isOffline) {
            args.add("--offline")
        }

        args.add("--ignore-scripts")
    }

    clean {
        dependsOn(cleanNpm, cleanBuild)
    }
}

configurations.wasmCustomFormattersProvider.configure {
    outgoing {
        artifact(npmBuild)
    }
}


/**
 * Extract node_modules dependencies from the `package-lock.json`.
 *
 * Convert each to GAV dependency coordinates.
 */
val nodeModuleDependencies = try {
    val packageLockFile = file("package-lock.json")
    JsonSlurper().parse(packageLockFile)
        .let { it as Map<*, *> }
        .let { it["packages"] as Map<*, *> }
        .let { packages ->
            packages.keys
                .map { dep -> dep as String }
                .filter { dep -> dep.startsWith("node_modules/") }
                .map { dep ->
                    val depData = packages[dep] as Map<*, *>
                    val version = depData["version"] as String

                    val module = dep.substringAfter("node_modules/").substringAfter("/")
                    val group = dep.substringAfter("node_modules/")
                        .replace("/", ".")
                    //.removePrefix("@")

                    //"npm.p.kt.kotlin-dependencies.$group:$module:$version@tgz"
                    "$group:$module:$version@tgz"
                }
        }
} catch (e: Exception) {
    System.err.println("Error parsing package-lock.json")
    throw e
}

val nodeModulesDependencies by configurations.registering {
    isCanBeDeclared = true
    isCanBeResolved = false
    isCanBeConsumed = false
    isVisible = false
    defaultDependencies {
        nodeModuleDependencies.forEach {
            add(project.dependencies.create(it))
        }
    }
}

val nodeModulesDependenciesResolver by configurations.registering {
    isCanBeDeclared = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false
    extendsFrom(nodeModulesDependencies.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("npm-dependencies-cache"))
    }
}

// use an object to avoid 'project script references' CC errors
object NpmUtil {

    /**
     * Util for executing npm.
     */
    fun npmExecProvider(
        nodeExtension: NodeExtension,
        objects: ObjectFactory,
        configure: NpmExecProviderSpec.() -> Unit,
    ): Provider<NpmExecResult> {

        val spec = objects.newInstance<NpmExecProviderSpec>()
            .apply(configure)

        return spec.providers.provider {
            val variantComputer = VariantComputer()
            val projectHelper = objects.newInstance<DefaultProjectApiHelper>()
            val npmExecRunner = objects.newInstance<NpmExecRunner>()

            val stdOut = ByteArrayOutputStream()
            val stdErr = ByteArrayOutputStream()

            val result = try {
                val nodeExecConfiguration =
                    NodeExecConfiguration(
                        command = spec.command.get(),
                        environment = spec.environment.orNull.orEmpty(),
                        workingDir = nodeExtension.nodeProjectDir.get().asFile,
                        ignoreExitValue = spec.ignoreExitValue.orNull ?: false,
                    ) {
                        standardOutput = stdOut
                        errorOutput = stdErr
                    }

                npmExecRunner.executeNpmCommand(
                    project = projectHelper,
                    extension = nodeExtension,
                    nodeExecConfiguration = nodeExecConfiguration,
                    variants = variantComputer,
                )
            } finally {
                stdOut.close()
                stdErr.close()
            }

            NpmExecResult(
                exitValue = result.exitValue,
                standardOutput = stdOut.toString(),
                errorOutput = stdErr.toString(),
            )
        }
    }

    abstract class NpmExecProviderSpec @Inject constructor(
        val providers: ProviderFactory,
    ) {
        abstract val command: ListProperty<String>
        abstract val environment: MapProperty<String, String>
        abstract val ignoreExitValue: Property<Boolean>

        /** Set the value of the [command] property. */
        fun command(vararg command: String) {
            this.command.set(listOf(*command))
        }
    }

    data class NpmExecResult(
        val exitValue: Int,
        val standardOutput: String,
        val errorOutput: String,
    ) : Serializable

}

val addNodeModulesToNpmCache by tasks.registering {
    val objects = serviceOf<ObjectFactory>()

    val nodeModulesDependenciesFiles = nodeModulesDependenciesResolver.map { it.incoming.files }
    inputs.files(nodeModulesDependenciesFiles)

    dependsOn(tasks.nodeSetup)

    val nodeExtension = project.node

    doLast {

        /**
         * Get all content of npm's cache, so we can avoid re-adding items to the cache (which is a little slow).
         */
        val cacheLsLines = run {
            val npmCacheLsResult = NpmUtil.npmExecProvider(nodeExtension, objects) {
                command("cache", "ls")
            }.get()

            logger.info("npm cache ls result :\n${npmCacheLsResult.toString().lines().joinToString("\\n ")}")

            npmCacheLsResult.standardOutput.lineSequence()
        }

        nodeModulesDependenciesFiles.get()
            .filter { dep ->
                // filter out dependencies that are already present in npm's cache
                cacheLsLines.none { it.endsWith(dep.invariantSeparatorsPath) }
            }
            .forEach { dep ->
                logger.lifecycle("Adding ${dep.name} to npm cache...")

                val result = NpmUtil.npmExecProvider(nodeExtension, objects) {
                    command("cache", "add", dep.invariantSeparatorsPath)
                }.get()

                logger.info("npm cache add result: ${result.toString().lines().joinToString("\\n ")}")
            }
    }
}

// region EXPERIMENT - run `npm install --dry-run --offline` as an up-to-date check for npmInstall
// https://github.com/node-gradle/gradle-node-plugin/issues/81


//    abstract class NpmExecSource @Inject internal constructor(
//        private val execOps: ExecOperations,
//    ) : ValueSource<NpmExecResult, NpmExecSource.Parameters> {
//
//        interface Parameters : ValueSourceParameters {
//            //        val nodeExtension: Property<NodeExtension>
//            val environment: MapProperty<String, String>
//            val ignoreExitValue: Property<Boolean>
//            val command: ListProperty<String>
//            val
////        val workingDir: DirectoryProperty
//        }
//
//        override fun obtain(): NpmExecResult {
//            val variantComputer = VariantComputer()
//            val projectHelper = objects.newInstance(DefaultProjectApiHelper::class)
//            val npmExecRunner = objects.newInstance(NpmExecRunner::class)
//
//            val stdOut = ByteArrayOutputStream()
//            val stdErr = ByteArrayOutputStream()
//
//            val nodeExtension = NodeExtension(ProjectBuilder.builder().build())
//
//            val result = try {
//                val nodeExecConfiguration =
//                    NodeExecConfiguration(
//                        command = parameters.command.get(),
//                        environment = parameters.environment.orNull.orEmpty(),
////                    workingDir = parameters.nodeExtension.get().nodeProjectDir.get().asFile,
//                        workingDir = nodeExtension.nodeProjectDir.get().asFile,
//                        ignoreExitValue = parameters.ignoreExitValue.orNull ?: false,
//                    ) {
//                        standardOutput = stdOut
//                        errorOutput = stdErr
//                    }
//
//                npmExecRunner.executeNpmCommand(
//                    project = projectHelper,
////                extension = parameters.nodeExtension.get(),
//                    extension =  nodeExtension ,
//                    nodeExecConfiguration = nodeExecConfiguration,
//                    variants = variantComputer,
//                )
//            } finally {
//                stdOut.close()
//                stdErr.close()
//            }
//
//            return NpmExecResult(
//                result.exitValue,
//                standardOutput = stdOut.toString(),
//                errorOutput = stdErr.toString(),
//            )
//        }
//    }

//val npmInstallUpToDateCheck by tasks.registering {
//    val nodeExtension = project.node
//    val objects = serviceOf<ObjectFactory>()
//    val npmInstallOfflineExecResult = NpmUtil.npmExecProvider(nodeExtension, objects) {
//        command = listOf("install", "--dry-run", "--offline")
//    }
//
//    val result = temporaryDir.resolve("result.txt")
//    outputs.file(result)
//
//    doLast {
//        println("[$path] checking if npm install is up to date...")
//        val isUpToDate: Boolean =
//            npmInstallOfflineExecResult.get().run {
//                println(npmInstallOfflineExecResult)
//                errorOutput.isEmpty()
//                        && standardOutput.lines().none { it.startsWith("add") }
//            }
//        println("[$path] result : $isUpToDate")
//        result.apply {
//            parentFile.mkdirs()
//            createNewFile()
//            writeText(isUpToDate.toString())
//        }
//    }
//}
////
//tasks.npmInstall {
////    val nodeExtension = project.node
////    val objects = serviceOf<ObjectFactory>()
//    val npmInstallOfflineExecResult = nodeExtension.npmExec {
//        command = listOf("install", "--dry-run", "--offline")
//    }
//
////    val upToDateCheckResult = npmInstallUpToDateCheck.map { it.outputs.files.singleOrNull()?.readText()?.toBoolean() ?: false }
//
//    outputs.upToDateWhen {
//        "missing dependencies" in npmInstallOfflineExecResult.get().standardOutput
////        npmInstallOfflineExecResult.get().run {
////            println(npmInstallOfflineExecResult)
////            errorOutput.isEmpty()
////                    && standardOutput.lines().none { it.startsWith("add") }
////        }
//    }
//}
//endregion
