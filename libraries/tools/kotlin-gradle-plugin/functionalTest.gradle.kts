import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

project.extensions.getByType<KotlinJvmProjectExtension>().target.compilations {
    val main by getting
    create("functionalTest") {
        associateWith(main)
        val functionalTest by tasks.register<Test>("functionalTest") {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs functional tests"
            testClassesDirs = output.classesDirs
            classpath = sourceSets["functionalTest"].runtimeClasspath
        }
        tasks.named("check") {
            dependsOn(functionalTest)
        }
    }
}

configurations.getByName("functionalTestImplementation") {
    extendsFrom(configurations.getByName("implementation"))
    extendsFrom(configurations.getByName("testImplementation"))
}

configurations.getByName("functionalTestRuntimeOnly") {
    extendsFrom(configurations.getByName("runtimeOnly"))
    extendsFrom(configurations.getByName("testRuntimeOnly"))
}
