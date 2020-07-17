package translators

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Text

fun DModule.documentationOf(className: String, functionName: String): String {
    return (packages.single()
        .classlikes.single { it.name == className }
        .functions.single { it.name == functionName }
        .documentation.values.singleOrNull()
        ?.children?.singleOrNull()
        .run { this as? Description }
        ?.root?.children?.single() as? Text)
        ?.body.orEmpty()
}