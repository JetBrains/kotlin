import Foundation

precondition(foo == 42)
precondition(bar == 0)
bar = foo
precondition(bar == 42)
precondition(baz == 42)
baz = 0
precondition(baz == 42)