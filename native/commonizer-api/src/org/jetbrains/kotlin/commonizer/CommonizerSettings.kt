/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.cli.OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_ALIAS
import org.jetbrains.kotlin.commonizer.cli.PLATFORM_INTEGER_COMMONIZATION_ENABLED_ALIAS
import java.io.Serializable

public interface CommonizerSettings {

    public sealed class Key<T : Any> {
        public abstract val defaultValue: T
        public abstract val alias: String
    }

    public fun <T : Any> getSetting(key: Key<T>): T
}

public object OptimisticNumberCommonizationEnabledKey : CommonizerSettings.Key<Boolean>() {
    override val defaultValue: Boolean = true
    override val alias: String
        get() = OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_ALIAS
}

public object PlatformIntegerCommonizationEnabledKey : CommonizerSettings.Key<Boolean>() {
    override val defaultValue: Boolean = false
    override val alias: String
        get() = PLATFORM_INTEGER_COMMONIZATION_ENABLED_ALIAS
}

// These settings affect the API produced by the Commonizer and therefore must be part of the cache identity
public data class CommonizationCacheAffectingSetting(
    internal val isOptimisticNumberCommonizationEnabled: Boolean,
    internal val isPlatformIntegerCommonizationEnabled: Boolean,
) : Serializable

public val CommonizationCacheAffectingSetting.commonizerArguments: List<AdditionalCommonizerSetting<*>> get() = listOf(
    OptimisticNumberCommonizationEnabledKey setTo isOptimisticNumberCommonizationEnabled,
    PlatformIntegerCommonizationEnabledKey setTo isPlatformIntegerCommonizationEnabled,
)

public val CommonizationCacheAffectingSetting.identityString: String get() = listOf(
    "${OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_ALIAS}=${isOptimisticNumberCommonizationEnabled}",
    "${PLATFORM_INTEGER_COMMONIZATION_ENABLED_ALIAS}=${isPlatformIntegerCommonizationEnabled}",
).joinToString(separator = ", ", prefix = "(", postfix = ")")