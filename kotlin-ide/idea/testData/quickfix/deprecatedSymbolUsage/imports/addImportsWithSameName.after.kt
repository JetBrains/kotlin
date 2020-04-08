// "Replace with 'gau()'" "true"

package test

import test.dependency.gau

@Deprecated("...", ReplaceWith("gau()", "test.dependency.gau"))
fun gau() {
    test.dependency.gau()
}

fun use() {
    gau()
}