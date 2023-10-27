// part-1: extension for class
extension MyClass {
    func demo() {
        foo()
        let b = bar(arg: Int32(1))
    }
}

// part-2: extension for build ins
extension Int32 {
    func demo() {
        foo()
        bar(arg: 1.1)
    }
}

// part-3: nested extensions
extension Bar {
    func demo() {
        let i: Int32 = 1
        var res: Int32 = foo1(receiver: i)
        res = foo2(receiver: i, arg: 1.1)
    }
}

// part-4: extensions in packages
extension extensions.MyNamespacedClass {
    func demo() {
        foo()
    }
}
