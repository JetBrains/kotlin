import org.jetbrains.kotlin.tools.projectWizard.core.Error
import org.jetbrains.kotlin.tools.projectWizard.core.asString
import org.yaml.snakeyaml.parser.ParserException

data class YamlParsingError(val exception: ParserException) : Error() {
    override val message: String
        get() = exception.asString()
}