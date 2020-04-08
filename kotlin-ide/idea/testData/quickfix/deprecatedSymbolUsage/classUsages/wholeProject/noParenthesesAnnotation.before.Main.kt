// "Replace usages of 'OldAnnotation' in whole project" "true"

package test

import dependency.OldAnnotation

fun foo(a: OldAnnotation) {
}

@<caret>OldAnnotation
class X