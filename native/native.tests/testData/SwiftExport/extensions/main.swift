// part-1: extension for class
let d = MyClass()
d.foo()
d.bar(arg: 1)

// part-2: extension for build ins
let i: Int32 = 1
i.foo()
i.bar(arg: 1.1)

// part-3: nexted extensions
let b = Bar()
var res: Int32 = b.foo1(receiver: i)
res = b.foo2(receiver: i, arg: 1.1)
