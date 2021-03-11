// "Add non-null asserted (!!) call" "true"
class Foo {
    val project: Project? = null

    fun quux() {
        baz(<caret>project)
    }

    fun baz(project: Project) {}

    class Project
}