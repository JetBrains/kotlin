import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.xcode.model.CoreXcodeWorkspace

object XcodeWorkspace : CoreXcodeWorkspace, Disposable {
    override fun dispose() = Unit

    val project: Project

    init {
        val applicationEnvironment = CoreApplicationEnvironment(this, false)
        val projectEnvironment = CoreProjectEnvironment(this, applicationEnvironment)
        project = projectEnvironment.project
    }
}
