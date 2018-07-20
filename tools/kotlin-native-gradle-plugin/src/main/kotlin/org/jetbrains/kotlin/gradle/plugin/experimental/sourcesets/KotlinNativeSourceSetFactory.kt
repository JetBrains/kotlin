/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
