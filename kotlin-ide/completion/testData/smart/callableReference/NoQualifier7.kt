import java.util.Date

fun foo(p: (Long) -> Date){}

fun bar(){
    foo(<caret>)
}

// EXIST: ::Date
