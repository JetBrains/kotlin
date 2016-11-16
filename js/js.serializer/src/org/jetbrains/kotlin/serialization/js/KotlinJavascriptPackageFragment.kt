/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

class KotlinJavascriptPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        private val loadResource: (path: String) -> InputStream?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    private val nameResolver =
            loadResourceSure(KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(fqName)).use { stream ->
                NameResolverImpl.read(stream)
            }

    override val classDataFinder = KotlinJavascriptClassDataFinder(nameResolver, loadResource)

    override fun computeMemberScope(): DeserializedPackageMemberScope =
            loadResourceSure(KotlinJavascriptSerializedResourcePaths.getPackageFilePath(fqName)).use { packageStream ->
                val packageProto = ProtoBuf.Package.parseFrom(packageStream, JsSerializerProtocol.extensionRegistry)
                DeserializedPackageMemberScope(
                        this, packageProto, nameResolver, containerSource = null, components = components,
                        classNames = { loadClassNames() }
                )
            }

    private fun loadClassNames(): Collection<Name> =
            loadResourceSure(KotlinJavascriptSerializedResourcePaths.getClassesInPackageFilePath(fqName)).use { classesStream ->
                val classesProto = JsProtoBuf.Classes.parseFrom(classesStream, JsSerializerProtocol.extensionRegistry)
                classesProto.classNameList?.map { id -> nameResolver.getName(id) } ?: listOf()
            }

    private fun loadResourceSure(path: String): InputStream =
            loadResource(path) ?: throw IllegalStateException("Resource not found in classpath: $path")
}
