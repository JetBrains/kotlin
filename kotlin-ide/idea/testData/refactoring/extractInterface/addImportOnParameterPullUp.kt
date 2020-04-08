// NAME: ISomething
// TARGET_FILE_NAME: addImportOnParameterPullUp.1.kt
import foo.Z

class <caret>C(
    // INFO: {checked: "true", toAbstract: "true"}
    val z: Z
)