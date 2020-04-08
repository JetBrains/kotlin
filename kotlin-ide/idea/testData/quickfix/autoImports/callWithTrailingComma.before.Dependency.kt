// "Import" "true"
// ERROR: Type mismatch: inferred type is Int but String was expected
// ACTION: Add 'toString()' call
// ACTION: Change parameter 'p' type of function 'foo' to 'Int'
// ACTION: Create function 'foo'
// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas

package other

import main.X

fun X.foo(p: Int) {
}
