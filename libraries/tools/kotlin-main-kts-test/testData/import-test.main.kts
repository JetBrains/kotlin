
@file:Import("import-common.main.kts")
@file:Import("import-middle.main.kts")

sharedVar = sharedVar + 1

println("${SharedObject.greeting} ${from.msg} main")
println("sharedVar == $sharedVar")
