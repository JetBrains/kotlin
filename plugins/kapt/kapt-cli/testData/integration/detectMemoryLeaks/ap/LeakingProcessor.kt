package apt

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno

class LeakingProcessor : AbstractProcessor() {
    companion object {
        @JvmField
        var leakedProcessor: Processor? = null
    }
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        leakedProcessor = this
    }

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean = true

    override fun getSupportedAnnotationTypes() =
        mutableSetOf("apt.Anno")

    override fun getSupportedSourceVersion() =
        SourceVersion.RELEASE_8

    override fun toString() = "LeakingProcessor"
}
