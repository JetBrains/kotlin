import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CreateDefFileTask : DefaultTask() {

    @get:OutputFile
    abstract val defFile: RegularFileProperty

    @TaskAction
    fun createDefFile() {
        val defFileContent =
            """
            headers = dummy.h
            compilerOpts = -Iinclude/libs

            package = my.cinterop
            ---

            static int foo() {
                return 42;
            }
            """.trimIndent()

        defFile.asFile.get().writeText(defFileContent)
    }
}