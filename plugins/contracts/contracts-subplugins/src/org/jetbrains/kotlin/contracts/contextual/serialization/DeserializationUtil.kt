/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.serialization

import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.subplugin.ContractSubpluginsProtoBuf
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

private val extensionRegistry = ExtensionRegistryLite.newInstance().apply {
    ContractSubpluginsProtoBuf.registerAllExtensions(this)
}

fun getExtendedProto(proto: ContractsProtoBuf.ContextProvider): ContractsProtoBuf.ContextProvider =
    ContractsProtoBuf.ContextProvider.parseFrom(proto.toByteArray(), extensionRegistry)

fun getExtendedProto(proto: ContractsProtoBuf.ContextVerifier): ContractsProtoBuf.ContextVerifier =
    ContractsProtoBuf.ContextVerifier.parseFrom(proto.toByteArray(), extensionRegistry)

fun getExtendedProto(proto: ContractsProtoBuf.ContextCleaner): ContractsProtoBuf.ContextCleaner =
    ContractsProtoBuf.ContextCleaner.parseFrom(proto.toByteArray(), extensionRegistry)