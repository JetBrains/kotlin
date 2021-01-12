// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.service.remote.CustomClassDeserializingResolver
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemProjectResolver

class ResolverDeserializationWrapper<S : ExternalSystemExecutionSettings>(
  val delegate: RemoteExternalSystemFacade<S>)
  : RemoteExternalSystemFacade<S> by delegate {
  override fun getResolver(): RemoteExternalSystemProjectResolver<S> =
    CustomClassDeserializingResolver<S>(delegate.rawProjectResolver, delegate.resolver)
}