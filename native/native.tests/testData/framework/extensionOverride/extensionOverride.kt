package extensionOverride

import receivers.Receiver

fun Receiver.fromFramework(): String = "framework " + name
