import custom.scriptDefinition.KotlinPlatformType.Companion.attribute

<selection>
val relocatedJarContents = configurations.creating {
    attributes {
        attribute(USAGE_ATTRIBUTE, JAVA_RUNTIME)
    }
}</selection>
