import java.io.File

fun foo(file: File?) {
    file?.<caret>
}

// EXIST: {"lookupString":"absolutePath","tailText":" (from getAbsolutePath())","typeText":"String","attributes":"bold","allLookupStrings":"absolutePath, getAbsolutePath","itemText":"absolutePath"}
// ABSENT: getAbsolutePath
