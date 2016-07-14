import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

val directory = "src/test/kotlin/tests"

fun main(args: Array<String>) {
    val mainFileWriter = File("$directory/linked/main.c").printWriter()
    mainFileWriter.println("#include <stdlib.h>\n" +
            "#include <stdio.h>\n" +
            "#include <assert.h>\n\n");


    mainFileWriter.println("\n")
    mainFileWriter.println("int main(){")

    for (input in File("$directory/input").listFiles()) {
        val inputLine = input.readLines()
        val outputLine = File("$directory/output/" + input.name).readLines()

        for (line in 0..inputLine.count() - 1) {
            mainFileWriter.println("\tassert (${inputLine.get(line)} == ${outputLine.get(line)} );")
            mainFileWriter.println("\tprintf(\"OK: ${inputLine.get(line)} == ${outputLine.get(line)} \\n\");")
        }
    }
    mainFileWriter.println("printf(\"TEST RESULT: OK\\n\");")
    mainFileWriter.println("\treturn 0;")
    mainFileWriter.println("}")
    mainFileWriter.flush()


    Runtime.getRuntime().exec("clang -S -emit-llvm $directory/linked/main.c -o $directory/linked/main.ll").waitFor()
    Runtime.getRuntime().exec("rm -f $directory/linked/main.c").waitFor()

    for (file in File("$directory/source").listFiles()) {
        val res = Runtime.getRuntime().exec("/usr/lib/jvm/default-java/bin/java -Didea.launcher.port=7539 " +
                "-Didea.launcher.bin.path=/opt/idea-IU-145.1617.8/bin -Dfile.encoding=UTF-8 " +
                "-classpath /usr/lib/jvm/default-java/jre/lib/charsets.jar:/usr/lib/jvm/default-java/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/default-java/jre/lib/ext/dnsns.jar:/usr/lib/jvm/default-java/jre/lib/ext/icedtea-sound.jar:/usr/lib/jvm/default-java/jre/lib/ext/jaccess.jar:/usr/lib/jvm/default-java/jre/lib/ext/localedata.jar:/usr/lib/jvm/default-java/jre/lib/ext/nashorn.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunec.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/default-java/jre/lib/ext/zipfs.jar:/usr/lib/jvm/default-java/jre/lib/jce.jar:/usr/lib/jvm/default-java/jre/lib/jsse.jar:/usr/lib/jvm/default-java/jre/lib/management-agent.jar:/usr/lib/jvm/default-java/jre/lib/resources.jar:/usr/lib/jvm/default-java/jre/lib/rt.jar:/home/user/Kotlin/carkot/translator/build/classes/main:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.0.1/68cddd9aec83d23f789f72bfdc933db245c4a635/kotlin-stdlib-1.0.1.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler/1.0.3/5dbeb14062996a5c2208861d6364cc97a21b06b8/kotlin-compiler-1.0.3.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-runtime/1.0.1/124852ea8cdd3d89827923b5e79627e8a7c314b2/kotlin-runtime-1.0.1.jar:/opt/idea-IU-145.1617.8/lib/idea_rt.jar " +
                "com.intellij.rt.execution.application.AppMain MainKt " + file.absoluteFile)
        res.waitFor()
        val reader = BufferedReader(InputStreamReader(res.getInputStream()))
        val currentFileWriter = File("$directory/linked/" + file.nameWithoutExtension + ".ll").printWriter()
        for (line in reader.lines()) {
            currentFileWriter.println(line)
        }
        currentFileWriter.flush()
    }
}
