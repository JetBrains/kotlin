import Foundation

let obj = NSObject()
let fubar = identity(obj: obj)

precondition(obj === fubar, "Object \(obj) returned as \(fubar) after passing through bridge")
