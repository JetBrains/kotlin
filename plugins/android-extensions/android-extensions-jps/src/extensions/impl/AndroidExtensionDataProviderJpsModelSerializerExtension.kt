/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.jps.android.model.base.impl

import org.jdom.Element
import org.jetbrains.jps.android.model.base.AndroidExtensionsDataProvider
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension

class AndroidExtensionDataProviderJpsModelSerializerExtension : JpsModelSerializerExtension() {
    override fun loadModuleOptions(module: JpsModule, rootElement: Element) {
        module.container.setChild(AndroidExtensionsDataProvider.KIND, AndroidExtensionsDataProviderImpl())
    }
}