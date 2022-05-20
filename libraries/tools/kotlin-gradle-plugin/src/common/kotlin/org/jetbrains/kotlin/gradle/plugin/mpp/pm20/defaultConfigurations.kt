/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category

val DefaultKotlinCompileDependenciesDefinition = GradleKpmConfigurationSetup(
    provider = GradleKpmConfigurationProvider {
        project.configurations.maybeCreate(disambiguateName("compileDependencies")).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
    },
    relations = GradleKpmConfigurationRelationSetup {
        extendsFrom(dependencies.transitiveApiConfiguration)
        extendsFrom(dependencies.transitiveImplementationConfiguration)
    },
    attributes = GradleKpmPlatformAttributes + GradleKpmConsumerApiUsageAttribute
)

val DefaultKotlinRuntimeDependenciesDefinition = GradleKpmConfigurationSetup(
    provider = GradleKpmConfigurationProvider {
        project.configurations.maybeCreate(disambiguateName("runtimeDependencies")).apply {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
    },
    attributes = GradleKpmPlatformAttributes + GradleKpmConsumerRuntimeUsageAttribute,
    relations = GradleKpmConfigurationRelationSetup {
        extendsFrom(dependencies.transitiveApiConfiguration)
        extendsFrom(dependencies.transitiveImplementationConfiguration)
        extendsFrom(dependencies.transitiveRuntimeOnlyConfiguration)
    }
)

val DefaultKotlinApiElementsDefinition = GradleKpmConfigurationSetup(
    provider = GradleKpmConfigurationProvider {
        project.configurations.maybeCreate(disambiguateName("apiElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            module.ifMadePublic { isCanBeConsumed = true }
        }
    },
    relations = GradleKpmConfigurationRelationSetup { extendsFrom(dependencies.transitiveApiConfiguration) },
    capabilities = GradleKpmModuleCapability,
    attributes = GradleKpmPlatformAttributes + GradleKpmProducerApiUsageAttribute + GradleKpmConfigurationAttributesSetup {
        attribute(Category.CATEGORY_ATTRIBUTE, fragment.project.objects.named(Category::class.java, Category.LIBRARY))
        attribute(Bundling.BUNDLING_ATTRIBUTE, fragment.project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
    },
)

val DefaultKotlinRuntimeElementsDefinition = GradleKpmConfigurationSetup(
    provider = GradleKpmConfigurationProvider {
        project.configurations.maybeCreate(disambiguateName("runtimeElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            module.ifMadePublic { isCanBeConsumed = true }
        }
    },
    relations = GradleKpmConfigurationRelationSetup {
        extendsFrom(dependencies.transitiveApiConfiguration)
        extendsFrom(dependencies.transitiveImplementationConfiguration)
        extendsFrom(dependencies.transitiveRuntimeOnlyConfiguration)
    },
    attributes = GradleKpmPlatformAttributes + GradleKpmProducerRuntimeUsageAttribute + GradleKpmConfigurationAttributesSetup {
        attribute(Category.CATEGORY_ATTRIBUTE, fragment.project.objects.named(Category::class.java, Category.LIBRARY))
        attribute(Bundling.BUNDLING_ATTRIBUTE, fragment.project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
    },
    capabilities = GradleKpmModuleCapability
)

val DefaultKotlinHostSpecificMetadataElementsDefinition = GradleKpmConfigurationSetup(
    provider = GradleKpmConfigurationProvider {
        project.configurations.maybeCreate(disambiguateName("hostSpecificMetadataElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
        }
    },
    attributes = GradleKpmPlatformAttributes + GradleKpmKonanTargetAttribute + GradleKpmMetadataUsageAttribute,
    artifacts = GradleKpmHostSpecificMetadataArtifact
)
