package org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.project.ProjectInternal

class KotlinNativeSourceSetFactory(val project: ProjectInternal) : NamedDomainObjectFactory<KotlinNativeSourceSet> {

    override fun create(name: String): KotlinNativeSourceSet =
            project.objects.newInstance(
                    KotlinNativeSourceSetImpl::class.java,
                    name,
                    project.services.get(SourceDirectorySetFactory::class.java),
                    project
            )
}
