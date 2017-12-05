/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil


open class KonanArtifactContainer(val project: ProjectInternal)
    : DefaultPolymorphicDomainObjectContainer<KonanBuildingConfig<*>>(
        KonanBuildingConfig::class.java,
        project.services.get(Instantiator::class.java)
) {

    init {
        registerFactory(KonanProgram::class.java) { name ->
            instantiator.newInstance(KonanProgram::class.java, name, project, instantiator)
        }
        registerFactory(KonanDynamic::class.java) { name ->
            instantiator.newInstance(KonanDynamic::class.java, name, project, instantiator)
        }
        registerFactory(KonanFramework::class.java) { name ->
            instantiator.newInstance(KonanFramework::class.java, name, project, instantiator)
        }
        registerFactory(KonanLibrary::class.java) { name ->
            instantiator.newInstance(KonanLibrary::class.java, name, project, instantiator)
        }
        registerFactory(KonanBitcode::class.java) { name ->
            instantiator.newInstance(KonanBitcode::class.java, name, project, instantiator)
        }
        registerFactory(KonanInteropLibrary::class.java) { name ->
            instantiator.newInstance(KonanInteropLibrary::class.java, name, project, instantiator)
        }
    }

    // TODO: Investigate if we can support the same DSL as project.task:
    //   program foo(targets: [target1, target2, target3]) {
    //       ...
    //   }

    fun program(name: String) = create(name, KonanProgram::class.java)
    fun program(name: String, configureAction: Action<KonanProgram>) =
            create(name, KonanProgram::class.java, configureAction)
    fun program(name: String, configureAction: KonanProgram.() -> Unit) =
            create(name, KonanProgram::class.java, configureAction)
    fun program(name: String, configureAction: Closure<*>) =
            program(name, ConfigureUtil.configureUsing(configureAction))

    fun dynamic(name: String) = create(name, KonanDynamic::class.java)
    fun dynamic(name: String, configureAction: Action<KonanDynamic>) =
            create(name, KonanDynamic::class.java, configureAction)
    fun dynamic(name: String, configureAction: KonanDynamic.() -> Unit) =
            create(name, KonanDynamic::class.java, configureAction)
    fun dynamic(name: String, configureAction: Closure<*>) =
            dynamic(name, ConfigureUtil.configureUsing(configureAction))

    fun framework(name: String) = create(name, KonanFramework::class.java)
    fun framework(name: String, configureAction: Action<KonanFramework>) =
            create(name, KonanFramework::class.java, configureAction)
    fun framework(name: String, configureAction: KonanFramework.() -> Unit) =
            create(name, KonanFramework::class.java, configureAction)
    fun framework(name: String, configureAction: Closure<*>) =
            framework(name, ConfigureUtil.configureUsing(configureAction))

    fun library(name: String) = create(name, KonanLibrary::class.java)
    fun library(name: String, configureAction: Action<KonanLibrary>) =
            create(name, KonanLibrary::class.java, configureAction)
    fun library(name: String, configureAction: KonanLibrary.() -> Unit) =
            create(name, KonanLibrary::class.java, configureAction)
    fun library(name: String, configureAction: Closure<*>) =
            library(name, ConfigureUtil.configureUsing(configureAction))

    fun bitcode(name: String) = create(name, KonanBitcode::class.java)
    fun bitcode(name: String, configureAction: Action<KonanBitcode>) =
            create(name, KonanBitcode::class.java, configureAction)
    fun bitcode(name: String, configureAction: KonanBitcode.() -> Unit) =
            create(name, KonanBitcode::class.java, configureAction)
    fun bitcode(name: String, configureAction: Closure<*>) =
            bitcode(name, ConfigureUtil.configureUsing(configureAction))

    fun interop(name: String) = create(name, KonanInteropLibrary::class.java)
    fun interop(name: String, configureAction: Action<KonanInteropLibrary>) =
            create(name, KonanInteropLibrary::class.java, configureAction)
    fun interop(name: String, configureAction: KonanInteropLibrary.() -> Unit) =
            create(name, KonanInteropLibrary::class.java, configureAction)
    fun interop(name: String, configureAction: Closure<*>) =
            interop(name, ConfigureUtil.configureUsing(configureAction))

}
