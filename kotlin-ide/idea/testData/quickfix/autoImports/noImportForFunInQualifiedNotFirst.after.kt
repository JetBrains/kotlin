// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ERROR: Unresolved reference: externalFun

package testing

fun some() {
  testing.<caret>externalFun()
}