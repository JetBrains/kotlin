import java.io.*;

File file = new File(basedir, "jlinked/target/maven-jlink/release")
if (!file.exists() || !file.isFile()) {
    throw new FileNotFoundException("Could not find generated image release file: " + file)
}

def releaseProps = new Properties()
releaseProps.load(new FileReader(file))

def modules = releaseProps.getProperty("MODULES")
assert modules != null : "MODULES property missing"

if (modules.startsWith("\"") && modules.endsWith("\"")) modules = modules[1..-2]

println("jlink has built an image with modules: $modules")

def moduleSet = modules.tokenize(" ").toSet()

for (module in ["java.base", "kotlin.stdlib", "kotlin.stdlib.jdk7", "kotlin.stdlib.jdk8", "org.test.modularApp"]) {
    assert moduleSet.contains(module) : "Expected to find $module in image modules: $modules"
}