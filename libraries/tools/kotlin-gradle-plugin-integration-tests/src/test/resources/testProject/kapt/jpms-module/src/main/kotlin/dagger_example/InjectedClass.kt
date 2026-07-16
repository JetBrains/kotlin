package dagger_example

import javax.inject.Inject

interface Injected {

    val message: String
}

class InjectedImpl @Inject constructor() : Injected {
    override val message = "This is injected1: " + SomeOtherClass().callMe()

    //placeholder
}
