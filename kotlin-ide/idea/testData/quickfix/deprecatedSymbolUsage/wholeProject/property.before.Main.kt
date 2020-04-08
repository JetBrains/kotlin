// "Replace usages of 'oldProp: String' in whole project" "true"

import pack.oldProp
import pack.foo

fun foo() {
    foo(<caret>oldProp)
}
