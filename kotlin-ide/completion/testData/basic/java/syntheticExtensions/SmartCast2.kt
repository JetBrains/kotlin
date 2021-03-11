import java.io.File

fun Any.foo() {
    if (this is File) {
        <caret>
    }
}

// EXIST: {"lookupString":"absolutePath","tailText":" (from getAbsolutePath())","typeText":"String","attributes":"bold","allLookupStrings":"absolutePath, getAbsolutePath","itemText":"absolutePath"}
// ABSENT: getAbsolutePath
