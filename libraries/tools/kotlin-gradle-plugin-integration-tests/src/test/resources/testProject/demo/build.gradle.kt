val debug by tasks.creating {
    doLast {
        println(System.getProperties().entries.joinToString("\n"))
        println(project.properties.entries.joinToString("\n"))
    }
}
