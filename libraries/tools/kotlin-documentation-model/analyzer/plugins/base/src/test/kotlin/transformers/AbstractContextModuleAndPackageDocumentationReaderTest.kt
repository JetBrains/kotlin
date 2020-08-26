package transformers

import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.withDescendants
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

abstract class AbstractContextModuleAndPackageDocumentationReaderTest {
    @TempDir
    protected lateinit var temporaryDirectory: Path


    companion object {
        val SourceSetDependent<DocumentationNode>.texts: List<String>
            get() = values.flatMap { it.withDescendants() }
                .flatMap { it.children }
                .flatMap { it.children }
                .mapNotNull { it as? Text }
                .map { it.body }
    }
}
