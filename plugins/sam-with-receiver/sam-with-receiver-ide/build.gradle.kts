import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":idea"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}


val jar: Jar by tasks

ideaPlugin {
    from(jar)
}

