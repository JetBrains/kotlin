// "Safe delete 'Imported'" "false"
// ACTION: Create test
// ACTION: Extract 'ImportedClass' from current file
// ACTION: Rename file to ImportedClass.kt
import ImportedClass as ClassAlias

class <caret>ImportedClass

fun use() {
    ClassAlias().hashCode()
}