package exportedAnotherBuild

import receivers.Receiver

fun Receiver.fromAnotherBuild(): String = "anotherBuild " + name
