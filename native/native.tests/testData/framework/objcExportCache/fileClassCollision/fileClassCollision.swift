import Foundation
import Kt

if UtilsKt.utilA() != "UtilA" {
    fatalError("Expected 'UtilA'")
}
if UtilsKt.utilB() != "UtilB" {
    fatalError("Expected 'UtilB'")
}

print("OK")
