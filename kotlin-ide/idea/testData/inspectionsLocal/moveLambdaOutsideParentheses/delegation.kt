// PROBLEM: none
interface I
class C1(s: String, f: (String) -> String) : I
class C2 : I by C1("", <caret>{ "" })
