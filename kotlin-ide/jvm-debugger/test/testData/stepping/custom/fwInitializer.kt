package fwInitializer

fun main(args: Array<String>) {
    A()
}

fun foo(): Int {
   return 1
}

//FieldWatchpoint! (topLevelProp)
val topLevelProp = foo()

class A {
    companion object {
        //FieldWatchpoint! (companionObjProp)
        val companionObjProp = foo()
    }

    //FieldWatchpoint! (prop)
    val prop = foo()
}

// RESUME: 2
// WATCH_FIELD_INITIALISATION: true
// WATCH_FIELD_MODIFICATION: false
// WATCH_FIELD_ACCESS: false