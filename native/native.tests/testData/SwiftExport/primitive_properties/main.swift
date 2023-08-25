import Foundation

precondition(foo == 42, "val foo expeted to be 42, got \(foo)")
precondition(bar == 0, "var bar expected to be initialized to 0, got \(bar)")
bar = foo
precondition(bar == 42, "var bar expected to be 42, got \(bar)")

precondition(baz == 42, "computed var baz expected to be 42, got \(baz)")
baz = 0
precondition(baz == 42, "computed var baz expected to be 42, got \(baz)")
precondition(bar == 0, "var bar expected to be 0, got \(bar)")

let object = NSObject()
precondition(obj !== object, "var obj expected to not be equal to \(object)")
obj = object
precondition(obj === object, "var obj expected to contain \(object), got \(obj)")