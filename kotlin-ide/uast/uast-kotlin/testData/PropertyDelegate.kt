val sdCardPath by lazy { "/sdcard" }

fun localPropertyTest() {
    val sdCardPathLocal by lazy { "/sdcard" }
}

@delegate:Suppress
val annotatedDelegate by lazy { 1 + 1 }