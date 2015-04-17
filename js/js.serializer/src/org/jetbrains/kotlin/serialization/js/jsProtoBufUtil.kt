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

import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import java.io.ByteArrayInputStream

public fun ByteArray.toPackageData(nameResolver: NameResolver): PackageData {
    val registry = KotlinJavascriptSerializedResourcePaths.EXTENSION_REGISTRY
    val packageProto = ProtoBuf.Package.parseFrom(ByteArrayInputStream(this), registry)
    return PackageData(nameResolver, packageProto)
}

public fun ByteArray.toClassData(nameResolver: NameResolver): ClassData {
    val registry = KotlinJavascriptSerializedResourcePaths.EXTENSION_REGISTRY
    val classProto = ProtoBuf.Class.parseFrom(ByteArrayInputStream(this), registry)
    return ClassData(nameResolver, classProto)
}

