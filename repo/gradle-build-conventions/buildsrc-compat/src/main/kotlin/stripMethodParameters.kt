@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.logging.Logger
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

/**
 * Removes the `MethodParameters` attribute from every class file under [directory].
 *
 * A modern `javac` writes that attribute whenever a parameter carries a flag worth recording — the mandated
 * outer instance of an inner class constructor, the parameters of a bridge method — even under `--release 8`,
 * and even though nothing in this build asks for parameter names. `javac` 8 wrote nothing at all. Those
 * entries have no name (`name_index = 0`, legal per JVMS §4.7.24), and two of the tools this repository still
 * depends on cannot cope with them:
 *
 * * the D8 that `dex-member-list` brings along for `dexMethodCount` predates the attribute and fails with a
 *   bare `NullPointerException`;
 * * JDK 8 builds before 8u4xx turn the missing name into `""` instead of `null`, so any `getParameters()`
 *   call throws `MalformedParametersException` — which breaks the test tasks that still run on JDK 8.
 *
 * Stripping the attribute restores what `javac` 8 used to produce. See [stripMetadata] for the same idea
 * applied to `@kotlin.Metadata`.
 *
 * @return the number of class files rewritten.
 */
fun stripMethodParameters(directory: File, logger: Logger? = null): Int {
    var rewritten = 0
    directory.walkTopDown().forEach { file ->
        if (!file.isFile || file.extension != "class") return@forEach
        val stripped = stripMethodParameters(file.readBytes()) ?: return@forEach
        file.writeBytes(stripped)
        rewritten++
    }
    if (rewritten > 0) {
        logger?.info("Stripped the MethodParameters attribute from $rewritten class file(s) in $directory")
    }
    return rewritten
}

/**
 * The bytes of [classFile] without its `MethodParameters` attributes, or `null` if it had none.
 *
 * ASM surfaces the attribute as [MethodVisitor.visitParameter], so dropping those calls is all it takes; the
 * unused `MethodParameters` string stays behind in the constant pool, which is harmless.
 */
private fun stripMethodParameters(classFile: ByteArray): ByteArray? {
    val reader = ClassReader(classFile)
    val writer = ClassWriter(reader, 0)
    var changed = false
    val visitor = object : ClassVisitor(Opcodes.API_VERSION, writer) {
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val delegate = super.visitMethod(access, name, descriptor, signature, exceptions)
            return object : MethodVisitor(Opcodes.API_VERSION, delegate) {
                override fun visitParameter(name: String?, access: Int) {
                    changed = true
                }
            }
        }
    }
    reader.accept(visitor, 0)
    return if (changed) writer.toByteArray() else null
}
