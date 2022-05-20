// EXPECTED_REACHABLE_NODES: 1282
// CHECK_COMMENT_EXISTS: text="Single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="Second single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="Third single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="Forth single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="Multi line comment" multiline=true
// CHECK_COMMENT_EXISTS: text="Single line comment inside function" multiline=false
// CHECK_COMMENT_EXISTS: text="Multi line comment inside function" multiline=true
// CHECK_COMMENT_EXISTS: text="After call single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="After call multi line comment" multiline=true
// CHECK_COMMENT_EXISTS: text="The header multiline\ncomment" multiline=true
// CHECK_COMMENT_DOESNT_EXIST: text="random position comment 1" multiline=true
// CHECK_COMMENT_DOESNT_EXIST: text="random position comment 2" multiline=true
// CHECK_COMMENT_DOESNT_EXIST: text="random position comment 3" multiline=true
// CHECK_COMMENT_EXISTS: text="1Multi line comment\n" multiline=true
// CHECK_COMMENT_EXISTS: text="2Multi line comment\n\n\n" multiline=true
// CHECK_COMMENT_EXISTS: text="3Multi line\n\n\n\n\ncomment\n" multiline=true
// CHECK_COMMENT_EXISTS: text="" multiline=true

package foo

fun box(): String {
    js("""
        /* The header multiline
        comment */
        function foo() {
            // Single line comment inside function
            Object;
            /*Multi line comment inside function*/
        }
        
        // Single line comment
        // Second single line comment
        foo();
        // Third single line comment
        // Forth single line comment
        
        /* Multi line comment */
        foo();
        
        foo(); // After call single line comment
        
        foo(); /* After call multi line comment */
        
        var /*random position comment 1*/ c /*random position comment 2*/ = /*random position comment 3*/ "Random position";
        
        /* 1Multi line comment 
        */
        foo();
        /* 2Multi line comment 


        */
        foo();
        /* 3Multi line


        
        
        comment 
        */
        foo();
        
        /**/
        foo();
    """)
    return "OK"
}