plugins {
    kotlin("jvm")
}

val main by sourceSets.getting {
    dependencies {
        api(kotlinStdlib())
        implementation(project(":native:hair:sym"))
        implementation(project(":native:hair:utils"))
        implementation(project(":native:hair:ir:core"))
    }
}

val generate by tasks.registering(JavaExec::class) {
    val generationRoot = projectDir.resolve("../generated/src/main/generated")
    doFirst {
        fun purgeDirectory(dir: File) {
            require(dir.exists()) { "$dir does not exist" }
            for (file in dir.listFiles()!!) {
                if (file.isDirectory) purgeDirectory(file)
                file.delete()
            }
        }

        purgeDirectory(generationRoot)
    }
    classpath = main.runtimeClasspath
    mainClass = "hair.ir.generator.MainKt"
    args = listOf(generationRoot.absolutePath)
}
