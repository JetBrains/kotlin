import com.moowork.gradle.node.npm.NpmTask

plugins {
    id("com.moowork.node").version("1.2.0")
}

apply { plugin("base") }

description = "Node utils"

node {
    download = true
}

val deployDir by extra { "${buildDir}/deploy_to_npm" }
val templateDir by extra { "${projectDir}/templates" }
val kotlincDir by extra { "${projectDir}/../../dist/kotlinc" }
val deployVersion by extra { System.getProperty("kotlin.deploy.version", "0.0.0") }
val deployTag by extra { System.getProperty("kotlin.deploy.tag", "dev") }
val authToken by extra { System.getProperty("kotlin.npmjs.auth.token") }
val dryRun by extra { System.getProperty("dryRun", "false") } // Pack instead of publish

fun Project.createCopyTemplateTask(templateName: String): Copy {
  return createTask("copy-${templateName}-template", Copy::class) {
      from("${templateDir}/${templateName}")
      into("${deployDir}/${templateName}")

      expand(hashMapOf("version" to deployVersion))
  }
}

fun Project.createCopyLibraryFilesTask(libraryName: String, fromJar: String): Copy {
  return createTask("copy-${libraryName}-library", Copy::class) {
    from(zipTree(fromJar).matching {
      include("${libraryName}.js")
      include("${libraryName}.meta.js")
      include("${libraryName}.js.map")
      include("${libraryName}/**")
    })

    into("${deployDir}/${libraryName}")
  }
}

fun Project.createPublishToNpmTask(templateName: String): NpmTask {
  return createTask("publish-${templateName}-to-npm", NpmTask::class) {
    val deployDir = File("${deployDir}/${templateName}")
    setWorkingDir(deployDir)

    val deployArgs = listOf("publish", "--//registry.npmjs.org/:_authToken=${authToken}", "--tag=${deployTag}")
    if (dryRun == "true") {
      println("${deployDir} \$ npm arguments: ${deployArgs}");
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
  createCopyLibraryFilesTask("kotlin", "${kotlincDir}/lib/kotlin-stdlib-js.jar"),
  createPublishToNpmTask("kotlin")
)

val publishKotlinCompiler = sequential(
  createCopyTemplateTask("kotlin-compiler"),
  createTask("copy-kotlin-compiler", Copy::class) {
    from("${kotlincDir}")
    into("${deployDir}/kotlin-compiler")
  },
  createTask("chmod-kotlinc-bin", Exec::class) {
    commandLine = listOf("chmod", "-R", "ugo+rx", "${deployDir}/kotlin-compiler/bin")
  },
  createPublishToNpmTask("kotlin-compiler")
)

val publishKotlinTest = sequential(
  createCopyTemplateTask("kotlin-test"),
  createCopyLibraryFilesTask("kotlin-test", "${kotlincDir}/lib/kotlin-test-js.jar"),
  createPublishToNpmTask("kotlin-test")
)

tasks {
  "publishAll" {
    dependsOn(publishKotlinJs, publishKotlinTest, publishKotlinCompiler)
  }
}
