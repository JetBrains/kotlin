import Foundation
import Kt

let item = SpecificItem(id: 42, tag: "test")
let desc = item.describe()
if desc != "Item #42" {
    fatalError("Expected 'Item #42', got '\(desc)'")
}

print("OK")
