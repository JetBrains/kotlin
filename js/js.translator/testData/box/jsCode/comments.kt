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
// CHECK_COMMENT_EXISTS: text="1Multi line comment\n" multiline=true
// CHECK_COMMENT_EXISTS: text="2Multi line comment\n\n\n" multiline=true
// CHECK_COMMENT_EXISTS: text="3Multi line\n\n\n\n\ncomment\n" multiline=true
// CHECK_COMMENT_EXISTS: text="" multiline=true
// CHECK_COMMENT_EXISTS: text="Multi line comment inside function" multiline=true
// CHECK_COMMENT_EXISTS: text="After call single line comment" multiline=false
// CHECK_COMMENT_EXISTS: text="After call multi line comment" multiline=true
// CHECK_COMMENT_EXISTS: text="Before argument 1" multiline=true
// CHECK_COMMENT_EXISTS: text="Before argument 2" multiline=true
// CHECK_COMMENT_EXISTS: text="After argument 1" multiline=true
// CHECK_COMMENT_EXISTS: text="After argument 2" multiline=true
// CHECK_COMMENT_EXISTS: text="object:" multiline=true
// CHECK_COMMENT_EXISTS: text="property:" multiline=true
// CHECK_COMMENT_EXISTS: text="descriptor:" multiline=true
// CHECK_COMMENT_EXISTS: text="Descriptor end" multiline=true

/*
* java.lang.AssertionError(Multi line comment with text 'The header multiline\ncomment' doesn't exist)
  java.lang.AssertionError(Multi line comment with text '1Multi line comment\n' doesn't exist)
  java.lang.AssertionError(Multi line comment with text '2Multi line comment\n\n\n' doesn't exist)
  java.lang.AssertionError(Multi line comment with text '3Multi line\n\n\n\n\ncomment\n' doesn't exist)
* */

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
        
        foo(
            /* Before argument 1 */
            /* Before argument 2 */
            4
            /* After argument 1 */
            /* After argument 2 */
        );
        
        var test = {
             test: Object.defineProperty(/* object: */{}, /* property: */'some_property', /* descriptor: */ {
              value: 42,
              writable: false
            } /* Descriptor end */)
        }
    """)
    return "OK"
}