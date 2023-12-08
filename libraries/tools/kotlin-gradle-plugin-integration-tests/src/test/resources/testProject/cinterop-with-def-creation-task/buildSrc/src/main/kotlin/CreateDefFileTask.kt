import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile

abstract class CreateDefFileTask : DefaultTask() {

    @get:OutputFile
    abstract val defFile: RegularFileProperty
}