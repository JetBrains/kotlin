import Foundation
import Kt

let user = User(name: "Bob")
let greeting = LibBKt.sayHello(user)
if greeting != "Hello, Bob" {
    fatalError("Expected 'Hello, Bob', got '\(greeting)'")
}

let categoryGreeting = user.sayHello()
if categoryGreeting != "Hello, Bob" {
    fatalError("Expected 'Hello, Bob', got '\(categoryGreeting)'")
}

print("OK")
