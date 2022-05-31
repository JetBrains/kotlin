// TEST_RUNNER: LLDB
// LLDB_CHECK_STEPS: main.kt:2 15
// FILE: main.kt

fun main() {

    inliner {
    
        println("1")
        
    }
    
    inliner {
        
        println("2")
        
    }

}


// FILE: inliner.kt
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
inline fun inliner(block: ()->Unit) {

    block()

}
    

