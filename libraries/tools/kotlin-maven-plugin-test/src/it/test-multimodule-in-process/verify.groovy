def buildLogFile = new File(basedir, "build.log")

def lines = buildLogFile.readLines()
def createdClassloaders = lines.count { it.startsWith("[DEBUG] Creating classloader") }

// 1 classloader for modules 1, 3, 4, 5
// 2 classloader for module 2 with a compiler plugin
return createdClassloaders == 2