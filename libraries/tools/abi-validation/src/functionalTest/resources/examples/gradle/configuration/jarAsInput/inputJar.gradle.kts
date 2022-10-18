tasks {
    jar {
        exclude("foo/HiddenField.class")
        exclude("foo/HiddenProperty.class")
    }
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}