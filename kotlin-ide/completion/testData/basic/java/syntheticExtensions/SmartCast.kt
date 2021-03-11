import java.io.File

fun foo(o: Any) {
    if (o is File) {
        o.<caret>
    }
}

// EXIST: {"lookupString":"absolutePath","tailText":" (from getAbsolutePath())","typeText":"String","attributes":"bold","allLookupStrings":"absolutePath, getAbsolutePath","itemText":"absolutePath"}
// ABSENT: getAbsolutePath
