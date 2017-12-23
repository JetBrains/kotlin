import com.moowork.gradle.node.npm.NpmTask

plugins {
  id("com.moowork.node").version("1.2.0")
  base
}

description = "Node utils"

node {
    download = true
}

val deployDir = "$buildDir/deploy_to_npm"
val templateDir = "$projectDir/templates"
val kotlincDir = rootProject.extra["distKotlinHomeDir"] as String

fun getProperty(name: String, default: String = "") = findProperty(name)?.toString() ?: default

val deployVersion = getProperty("kotlin.deploy.version", "0.0.0")
val deployTag = getProperty("kotlin.deploy.tag", "dev")
val authToken = getProperty("kotlin.npmjs.auth.token")
val dryRun = getProperty("dryRun", "false") // Pack instead of publish

fun Project.createCopyTemplateTask(templateName: String): Copy {
  return task<Copy>("copy-$templateName-template") {
      from("$templateDir/$templateName")
      into("$deployDir/$templateName")

      expand(hashMapOf("version" to deployVersion))
  }
}

fun Project.createCopyLibraryFilesTask(libraryName: String, fromJar: String): Copy {
  return task<Copy>("copy-$libraryName-library") {
    from(zipTree(fromJar).matching {
      include("$libraryName.js")
      include("$libraryName.meta.js")
      include("$libraryName.js.map")
      include("$libraryName/**")
    })

    into("$deployDir/$libraryName")
  }
}

fun Project.createPublishToNpmTask(templateName: String): NpmTask {
  return task<NpmTask>("publish-$templateName-to-npm") {
    val deployDir = File("$deployDir/$templateName")
    setWorkingDir(deployDir)

    val deployArgs = listOf("publish", "--//registry.npmjs.org/:_authToken=$authToken", "--tag=$deployTag")
    if (dryRun == "true") {
      println("$deployDir \$ npm arguments: $deployArgs");
      setArgs(listOf("pack"))
    }
    else {
      setArgs(deployArgs)
    }
  }
}

fun sequential(first: Task, vararg tasks: Task): Task {
  tasks.fold(first) { previousTask, currentTask ->
    currentTask.dependsOn(previousTask)
  }
  return tasks.last()
}

val publishKotlinJs = sequential(
        createCopyTemplateTask("kotlin"),
        createCopyLibraryFilesTask("kotlin", "$kotlincDir/lib/kotlin-stdlib-js.jar"),
        createPublishToNpmTask("kotlin")
)

val publishKotlinCompiler = sequential(
  createCopyTemplateTask("kotlin-compiler"),
  task<Copy>("copy-kotlin-compiler") {
    from(kotlincDir)
    into("$deployDir/kotlin-compiler")
  },
  task<Exec>("chmod-kotlinc-bin") {
    commandLine = listOf("chmod", "-R", "ugo+rx", "$deployDir/kotlin-compiler/bin")
  },
  createPublishToNpmTask("kotlin-compiler")
)

val publishKotlinTest = sequential(
        createCopyTemplateTask("kotlin-test"),
        createCopyLibraryFilesTask("kotlin-test", "$kotlincDir/lib/kotlin-test-js.jar"),
        createPublishToNpmTask("kotlin-test")
)

task("publishAll") {
    dependsOn(publishKotlinJs, publishKotlinTest, publishKotlinCompiler)
}
