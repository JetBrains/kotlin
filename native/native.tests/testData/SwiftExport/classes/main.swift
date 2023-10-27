import Foundation

precondition(classes.Foo.self != classes.Bar.self) // this should just compile

let foo = classes.Foo()
let bar = classes.Bar()

precondition(foo !== bar)

let baz = classes.Foo.FooClass(value: 42)
precondition(baz.value == 42)