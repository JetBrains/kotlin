plugins {
    kotlin("jvm")
}

jvmTarget = "1.8"

dependencies {
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(intellijCore())

}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
