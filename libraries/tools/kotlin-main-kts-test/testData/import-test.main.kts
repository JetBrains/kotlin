
@file:Import("import-common.main.kts")
@file:Import("import-middle.main.kts")

sharedVar = sharedVar + 1

println("sharedVar == $sharedVar")
