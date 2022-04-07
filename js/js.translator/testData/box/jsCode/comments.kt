// EXPECTED_REACHABLE_NODES: 1282
// CHECK_COMMENT_EXISTS: text="Single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="Multi line comment" multiline=true
// CHECK_COMMENT_EXISTS: text="Single line comment inside function" multiline=false
// CHECK_COMMENT_EXISTS: text="Multi line comment inside function" multiline=true
// CHECK_COMMENT_EXISTS: text="After call single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="After call multi line comment" multiline=true
// CHECK_COMMENT_DOESNT_EXIST: text="random position comment 1" multiline=true
// CHECK_COMMENT_DOESNT_EXIST: text="random position comment 2" multiline=true
// CHECK_COMMENT_DOESNT_EXIST: text="random position comment 3" multiline=true

package foo

fun box(): String {
    js("""
        function foo() {
            // Single line comment inside function
            Object;
            /*Multi line comment inside function*/
        }
        
        // Single line comment
        foo();
        
        /* Multi line comment */
        foo();
        
        foo(); // After call single line comment
        
        foo(); /* After call multi line comment */
        
        var /*random position comment 1*/ c /*random position comment 2*/ = /*random position comment 3*/ "Random position";
    """)
    return "OK"
}