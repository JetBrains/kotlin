plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-gradle-plugin-idea"))
    implementation("com.google.protobuf:protobuf-java:3.19.4")
    implementation("com.google.protobuf:protobuf-kotlin:3.19.4")
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
}

configureKotlinCompileTasksGradleCompatibility()

sourceSets.main.configure {
    java.srcDir("src/generated/java")
    java.srcDir("src/generated/kotlin")
}

tasks.register<Exec>("protoc") {
    val protoSources = "src/main/proto"
    val javaOutput = file("src/generated/java/")
    val kotlinOutput = file("src/generated/kotlin/")

    inputs.dir(protoSources)
    outputs.dir(javaOutput)
    outputs.dir(kotlinOutput)

    doFirst {
        javaOutput.deleteRecursively()
        kotlinOutput.deleteRecursively()
        javaOutput.mkdirs()
        kotlinOutput.mkdirs()
    }

    workingDir(project.projectDir)

    commandLine(
        "protoc",
        "-I=$protoSources",
        "--java_out=${javaOutput.absolutePath}",
        "--kotlin_out=${kotlinOutput.absolutePath}",
        "src/main/proto/module_coordinates.proto",
        "src/main/proto/extras.proto"
    )

    doLast {
        kotlinOutput.walkTopDown()
            .filter { it.extension == "kt" }
            .forEach { file -> file.writeText(file.readText().replace("public", "internal")) }

        /*
        javaOutput.walkTopDown()
            .filter { it.extension == "java" }
            .forEach { file -> file.writeText(file.readText().replace("public final class", "final class")) }
         */
    }
}
