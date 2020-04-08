fun foo(array: Array<String?>){}

fun f(){
    foo(<caret>)
}

// EXIST: { lookupString: "arrayOf", tailText: "(vararg elements: String?) (kotlin)", typeText: "Array<String?>" }
// EXIST: { lookupString: "arrayOfNulls", tailText: "(size: Int) (kotlin)", typeText: "Array<String?>" }
