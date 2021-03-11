// ERROR: Unresolved reference: Dependency
// ERROR: Unresolved reference: topLevelFun
// ERROR: Unresolved reference: extensionFun
// ERROR: Unresolved reference: extensionProp
// ERROR: Unresolved reference: dependency
package to


fun foo(d: Dependency) {
    topLevelFun()
    "".extensionFun()
    println("".extensionProp)
    dependency.Dependency()
}
