/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager

fun createKotlinJavascriptPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFragmentProtos: List<ProtoBuf.PackageFragment>,
        configuration: DeserializationConfiguration
): PackageFragmentProvider {
    val packageFragments = packageFragmentProtos.map { proto ->
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
        val fqName = when {
            proto.hasPackage() -> nameResolver.getPackageFqName(proto.`package`.getExtension(JsProtoBuf.packageFqName))
            proto.class_Count > 0 -> nameResolver.getClassId(proto.class_OrBuilderList.first().fqName).packageFqName
            else -> throw IllegalStateException("Invalid library part: either a Package or a Class must be present")
        }
        KotlinJavascriptPackageFragment(fqName, storageManager, module, proto, nameResolver)
    }

    val provider = PackageFragmentProviderImpl(packageFragments)

    val notFoundClasses = NotFoundClasses(storageManager, module)

    val components = DeserializationComponents(
            storageManager,
            module,
            configuration,
            DeserializedClassDataFinder(provider),
            AnnotationAndConstantLoaderImpl(module, notFoundClasses, JsSerializerProtocol),
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            DynamicTypeDeserializer,
            emptyList(),
            notFoundClasses
    )

    for (packageFragment in packageFragments) {
        packageFragment.components = components
    }

    return provider
}
