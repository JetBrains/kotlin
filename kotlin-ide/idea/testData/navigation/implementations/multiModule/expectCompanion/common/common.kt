package test

expect class Owner {
    companion <caret>object {

    }
}

// REF: companion object of [testModule_JVM] (test).Owner
// REF: companion object of [testModule_JS] (test).Owner
