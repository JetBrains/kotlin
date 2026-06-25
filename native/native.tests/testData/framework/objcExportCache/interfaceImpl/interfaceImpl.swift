import Foundation
import Kt

let greeter: Greeter = LibBKt.createGreeter()
let message = greeter.greet(name: "World")
if message != "Hello, World!" {
    fatalError("Expected 'Hello, World!', got '\(message)'")
}

let englishGreeter = EnglishGreeter()
let message2 = englishGreeter.greet(name: "Swift")
if message2 != "Hello, Swift!" {
    fatalError("Expected 'Hello, Swift!', got '\(message2)'")
}

print("OK")
