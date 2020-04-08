// "Delete redundant extension property" "true"
import java.io.File

val File.<caret>name: String
    get() = getName()