package apt

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


annotation class Annotation1
annotation class Annotation2
annotation class Annotation3

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("apt.Annotation1")
class AnnotationProcessor1 : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(Annotation1::class.java)
        for (element in elements) {
            val generatedSimpleName = "${element.simpleName}1"

            val file = processingEnv.filer.createSourceFile("generated.$generatedSimpleName")

            file.openWriter().use {
                it.write("package generated;\n@apt.Annotation2\npublic class $generatedSimpleName {}")
            }
        }

        return true
    }
}

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("apt.Annotation2")
class AnnotationProcessor2 : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(Annotation2::class.java)
        for (element in elements) {
            val generatedSimpleName = "${element.simpleName}2"

            val file = processingEnv.filer.createSourceFile("generated.$generatedSimpleName")

            file.openWriter().use {
                it.write("package generated;\n@apt.Annotation3\npublic class $generatedSimpleName {}")
            }
        }

        return true
    }
}

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("apt.Annotation3")
class AnnotationProcessor3 : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(Annotation3::class.java)
        for (element in elements) {
            val generatedSimpleName = "${element.simpleName}3"

            val file = processingEnv.filer.createSourceFile("generated.$generatedSimpleName")

            file.openWriter().use {
                it.write("package generated;\npublic class $generatedSimpleName {}")
            }
        }

        return true
    }
}