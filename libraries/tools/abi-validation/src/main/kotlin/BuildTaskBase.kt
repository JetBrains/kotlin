/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import org.gradle.api.DefaultTask
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

public abstract class BuildTaskBase : DefaultTask() {
    private val extension = project.apiValidationExtensionOrNull

    private fun stringSetProperty(provider: ApiValidationExtension.() -> Set<String>): SetProperty<String> {
        return project.objects.setProperty(String::class.java).convention(
            project.provider {
                if (extension == null) {
                    emptySet<String>()
                } else {
                    provider(extension)
                }
            }
        )
    }

    @get:Input
    public val ignoredPackages: SetProperty<String> = stringSetProperty { ignoredPackages }

    @get:Input
    public val nonPublicMarkers: SetProperty<String> = stringSetProperty { nonPublicMarkers }

    @get:Input
    public val ignoredClasses: SetProperty<String> = stringSetProperty { ignoredClasses }

    @get:Input
    public val publicPackages: SetProperty<String> = stringSetProperty { publicPackages }

    @get:Input
    public val publicMarkers: SetProperty<String> = stringSetProperty { publicMarkers }

    @get:Input
    public val publicClasses: SetProperty<String> = stringSetProperty { publicClasses }

    @get:Internal
    internal val projectName = project.name
}
