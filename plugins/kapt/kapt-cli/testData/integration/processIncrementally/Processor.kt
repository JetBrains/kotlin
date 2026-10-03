package apt

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileObject

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Generate

class IncrementalProcessor : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf("apt.Generate")

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.latestSupported()

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        if (roundEnv.processingOver()) return false

        for (element in roundEnv.getElementsAnnotatedWith(Generate::class.java)) {
            if (element.kind != ElementKind.CLASS) continue

            val input = element as TypeElement
            val packageName = processingEnv.elementUtils
                .getPackageOf(input)
                .qualifiedName
                .toString()

            val generatedName = "${input.simpleName}Generated"
            val qualifiedName =
                if (packageName.isEmpty()) generatedName
                else "$packageName.$generatedName"

            val file: JavaFileObject =
                processingEnv.filer.createSourceFile(qualifiedName, input)

            file.openWriter().use { writer ->
                if (packageName.isNotEmpty()) {
                    writer.write("package $packageName;\n\n")
                }

                writer.write("public final class $generatedName {\n")
                writer.write("}\n")
            }
        }

        return true
    }
}
