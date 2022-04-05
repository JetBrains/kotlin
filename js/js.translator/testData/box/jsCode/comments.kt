// EXPECTED_REACHABLE_NODES: 1282
package foo

fun simpleSingleLineComment() {
    js("""
       // This is 'simpleSingleLineComment' comment  
       console.log('simpleSingleLineComment');
    """)
}

fun simpleMultilineComment() {
    js("""
       /* 
        This is 'simpleMultilineComment' comment 
        */
       console.log('simpleSingleLineComment');
    """)
}

fun complexMultilineComment() {
    js("""
       function mul(a/*: float*/, b/*: float*/)/*: int*/ {
          return /*int(*/a/*)*/ * /*int(*/b/*)*/;
       }
    """)
}

fun box(): String {
    simpleSingleLineComment()
    simpleMultilineComment()
    complexMultilineComment()
    return "OK"
}