import SwiftUI
import Shared
import Subproject

struct Data {
    func xcode_foo() -> Int32 {
        com.github.jetbrains.example.foo()
    }

    func xcode_bar() -> Int32 {
        com.github.jetbrains.example.bar()
    }

    func xcode_foobar() -> Int32 {
        com.github.jetbrains.example.foobar(param: 5)
    }

    func xcode_fromSubproject() -> Int32 {
        com.github.jetbrains.library.libraryFoo()
    }
}

struct ContentView: View {
    let data = Data()
    var body: some View {
        VStack(spacing: 8, content: {
            Text("Test")
            Text("foo: \(data.xcode_foo())")
            Text("bar: \(data.xcode_bar())")
            Text("foobar 5: \(data.xcode_foobar())")
            Text("fromSubproject: \(data.xcode_fromSubproject())")
        })
    }
}

