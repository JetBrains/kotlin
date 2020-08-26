package translators

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.Text
import java.util.NoSuchElementException

fun DModule.documentationOf(className: String, functionName: String? = null): String =
    descriptionOf(className, functionName)
        ?.firstChildOfType<Text>()
        ?.body.orEmpty()

fun DModule.descriptionOf(className: String, functionName: String? = null): Description? {
    val classlike = packages.single()
        .classlikes.single { it.name == className }
    val target: Documentable =
        if (functionName != null) classlike.functions.single { it.name == functionName } else classlike
    return target.documentation.values.singleOrNull()
        ?.firstChildOfTypeOrNull<Description>()
}

fun DModule.findPackage(packageName: String? = null) =
    packageName?.let { packages.firstOrNull { pkg -> pkg.packageName == packageName }
        ?: throw NoSuchElementException("No packageName with name $packageName") } ?: packages.single()

fun DModule.findClasslike(packageName: String? = null, className: String? = null): DClasslike {
    val pkg = findPackage(packageName)
    return className?.let {
        pkg.classlikes.firstOrNull { cls -> cls.name == className }
            ?: throw NoSuchElementException("No classlike with name $className")
    } ?: pkg.classlikes.single()
}

fun DModule.findFunction(packageName: String? = null, className: String, functionName: String? = null): DFunction {
    val classlike = findClasslike(packageName, className)
    return functionName?.let {
        classlike.functions.firstOrNull { fn -> fn.name == functionName }
            ?: throw NoSuchElementException("No classlike with name $functionName")
    } ?: classlike.functions.single()
}
