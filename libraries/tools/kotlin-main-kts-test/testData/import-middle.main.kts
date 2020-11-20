
@file:Import("import-common.main.kts")

sharedVar *= 2

val from = SharedClass("from")

println("${SharedObject.greeting} ${from.msg} middle")
