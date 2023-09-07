import Foundation

precondition(namespaces.bar.foo() == 10)
precondition(namespaces.bar.bar == 10)

precondition(namespaces.foo.foo() == 20)
precondition(namespaces.foo.bar == 20)

precondition(foo() == 0)
precondition(bar == 0)