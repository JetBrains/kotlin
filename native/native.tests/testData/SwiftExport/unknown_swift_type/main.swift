import Foundation

class MySwiftClass {}
struct MyStruct {}

precondition(checkIfMySwiftClass(a: MySwiftClass()))
precondition(checkIfNSTaggedPointerString(a: "MyString" as AnyObject))
precondition(checkIfSwiftValue(a: MyStruct() as AnyObject))
precondition(areTypesTheSame(a: MyStruct() as AnyObject, b: MyStruct() as AnyObject))
precondition(!areTypesTheSame(a: MyStruct() as AnyObject, b: MySwiftClass()))