// "Fix 'kotlin.dom' and 'kotlin.browser' packages usages in the project" "true"
// JS

package test

import kotlin.<caret>browser.localStorage
import kotlin.dom.addClass

fun usage() {
    kotlin.browser.document.toString()
}