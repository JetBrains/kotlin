import com.github.gradle.node.npm.task.NpmTask
import org.gradle.kotlin.dsl.register

plugins {
    alias(libs.plugins.gradle.node)
    base
}

description = "Node utils"

node {
    download.set(true)
    distBaseUrl.set(null as String?)
}

val deployDir = "${layout.buildDirectory.get().asFile}/deploy_to_npm"
val templateDir = "$projectDir/templates"
val kotlincDir = "$projectDir/../../dist/kotlinc"

fun getProperty(name: String, default: String = "") = findProperty(name)?.toString() ?: default

val deployVersion = getProperty("kotlin.deploy.version", "0.0.0")
val deployTag = getProperty("kotlin.deploy.tag", "dev")
val authToken = getProperty("kotlin.npmjs.auth.token")
val dryRun = getProperty("dryRun", "false") // Pack instead of publish

fun Project.createCopyTemplateTask(templateName: String): TaskProvider<Copy> {
    return tasks.register<Copy>("copy-$templateName-template") {
        from("$templateDir/$templateName")
        into("$deployDir/$templateName")

        expand(hashMapOf("version" to deployVersion))
    }
}

val createCopyTemplate = createCopyTemplateTask("kotlin-compiler")

val copyKotlinCompiler = tasks.register<Copy>("copy-kotlin-compiler") {
    dependsOn(createCopyTemplate)
    from(kotlincDir)
    into("$deployDir/kotlin-compiler")
}

val makeBinExecutable = tasks.register<Exec>("chmod-kotlinc-bin") {
    dependsOn(copyKotlinCompiler)
    commandLine = listOf("chmod", "-R", "ugo+rx", "$deployDir/kotlin-compiler/bin")
}

val npmWhoami = createWhoamiNpmTask()

fun Project.createPublishToNpmTask(templateName: String): TaskProvider<NpmTask> {
    return tasks.register<NpmTask>("publish-$templateName-to-npm") {
        dependsOn(makeBinExecutable)
        val deployDir = File("$deployDir/$templateName")
        workingDir.set(deployDir)

        val deployArgs = listOf("publish", "--//registry.npmjs.org/:_authToken=$authToken", "--tag=$deployTag")
        if (dryRun == "true") {
            println("$deployDir \$ npm arguments: $deployArgs");
            args.set(listOf("pack"))
            dependsOn(npmWhoami)
        } else {
            args.set(deployArgs)
        }
    }
}

fun Project.createWhoamiNpmTask(): TaskProvider<NpmTask> {
    return tasks.register<NpmTask>("npm-whoami") {
        args.set(listOf("whoami", "--//registry.npmjs.org/:_authToken=$authToken"))
    }
}

val publishKotlinCompiler = createPublishToNpmTask("kotlin-compiler")

tasks.register("publishAll") {
    dependsOn(publishKotlinCompiler)
}
