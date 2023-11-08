import Foundation

let foo: interfaces.Foo & interfaces.Bar = interfaces.Impl() // this should just compile

precondition(foo.bar() == 42)