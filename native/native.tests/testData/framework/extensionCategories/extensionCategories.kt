package extensionCategories

import receivers.Receiver

fun Receiver.fromFramework(): String = "framework " + name

class Local

fun Local.fromFrameworkOnLocal(): String = "local"
