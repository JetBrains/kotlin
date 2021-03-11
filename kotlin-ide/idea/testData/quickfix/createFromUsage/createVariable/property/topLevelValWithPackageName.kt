// "Create property 'foo'" "true"
// ERROR: Property must be initialized

package foo

fun test(): Int {
    return <caret>foo
}
