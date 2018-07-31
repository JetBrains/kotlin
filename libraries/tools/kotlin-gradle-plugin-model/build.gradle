apply plugin: 'kotlin'

configureJvmProject(project)
configurePublishing(project)

repositories {
    mavenLocal()
}

dependencies {
    compile project(':kotlin-stdlib')
}

artifacts {
    archives sourcesJar
    archives javadocJar
}

jar {
    manifestAttributes(manifest, project)
}