import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    metadata {}
    jvm {
        withJava()
        compilations["main"].dependencies {
            compileOnly(kotlinStdlib())
        }
    }

    js(LEGACY) {
        nodejs()

        compilations {
            all {
                kotlinOptions {
                    main = "noCall"
                    moduleKind = "commonjs"
                }
            }

            @Suppress("UNCHECKED_CAST")
            this as NamedDomainObjectContainerScope<KotlinJsCompilation> // fixes type inference for getting dsl
            val main by getting {
                kotlinOptions {
                    outputFile = "${buildDir}/classes/js-v1/main/kotlin.js"
                    sourceMap = true
                }
            }

            val runtime by creating {
                kotlinOptions {
                    metaInfo = false
                    outputFile = "${buildDir}/classes/js-v1/runtime/kotlin.js"
                    sourceMap = true
                }
            }

            val test by getting {
                kotlinOptions {
                    moduleKind = "umd"
                }
            }
        }
    }
    js("jsIr", IR) {
        nodejs {}

        // Can't add to regular compileOnlyConfiguration, because packageJson task will fail to configure
        (compilations["main"].compileDependencyFiles as Configuration).dependencies.add(kotlinStdlib("js") as Dependency)
    }

    // wasm -- TODO
    // native -- is bundled to konan

    sourceSets {
        val commonMain by getting
        val commonJsMain by creating
        val jsMain by getting
        val jsIrMain by getting
        commonJsMain.dependsOn(commonMain)
        jsMain.dependsOn(commonJsMain)
        jsIrMain.dependsOn(commonJsMain)
    }
}



publishing {
    repositories {
        maven(projectDir.resolve("repo"))
    }

    // Disable default kotlin multiplatform publishing
    val defaultKotlinPublishings = listOf("kotlinMultiplatform", "jvm", "js", "jsIr")
    afterEvaluate {
        tasks.withType<AbstractPublishToMaven>().all {
            if (publication.name in defaultKotlinPublishings) {
                enabled = false
            }
        }
    }


    configureMultiModuleMavenPublishing {
        val rootModule = module("rootModule") {
            mavenPublication {
                groupId = "org.example"
                artifactId = "sample-lib"
                pom {
                    // TODO: Configure pom
                }
            }

            // creates a variant from existing configuration or creates new one
            variant("jvmApiElements")
            variant("jvmRuntimeElements")
            variant("jvmSourcesElements")

            variant("metadataApiElements")
//            variant("metadataSourcesElements")
        }

        val common = module("commonModule") {
            mavenPublication {
                groupId = "org.example"
                artifactId = "sample-lib-common"
            }
            variant("commonMainMetadataElements") {
                // Multiplatform KGP already added klib artifact to metadataApiElements
                // attributes { kotlinLegacyMetadataAttributes() }
            }
        }
        val js = module("jsModule") {
            mavenPublication {
                groupId = "org.example"
                artifactId = "sample-lib-js"
            }
            variant("jsApiElements")
            variant("jsRuntimeElements")
            variant("jsIrApiElements")
            variant("jsIrRuntimeElements")
        }

        // Makes all variants from common and js be visible through `available-at`
        rootModule.include(common, js)
    }
}

class MultiModuleMavenPublishingConfiguration() {
    val modules = mutableMapOf<String, Module>()

    class Module(val name: String) {
        val variants = mutableMapOf<String, Variant>()
        val includes = mutableSetOf<Module>()

        class Variant(
            val name: String
        ) {
            val attributesConfigurations = mutableListOf<AttributeContainer.() -> Unit>()
            fun attributes(code: AttributeContainer.() -> Unit) {
                attributesConfigurations += code
            }

            val artifactsWithConfigurations = mutableListOf<Pair<Any, ConfigurablePublishArtifact.() -> Unit>>()
            fun artifact(file: Any, code: ConfigurablePublishArtifact.() -> Unit = {}) {
                artifactsWithConfigurations += file to code
            }

            val configurationConfigurations = mutableListOf<Configuration.() -> Unit>()
            fun configuration(code: Configuration.() -> Unit) {
                configurationConfigurations += code
            }

            val variantDetailsConfigurations = mutableListOf<ConfigurationVariantDetails.() -> Unit>()
            fun configureVariantDetails(code: ConfigurationVariantDetails.() -> Unit) {
                variantDetailsConfigurations += code
            }
        }

        val mavenPublicationConfigurations = mutableListOf<MavenPublication.() -> Unit>()
        fun mavenPublication(code: MavenPublication.() -> Unit) {
            mavenPublicationConfigurations += code
        }

        fun variant(name: String, code: Variant.() -> Unit = {}): Variant {
            val variant = variants.getOrPut(name) { Variant(name) }
            variant.code()
            return variant
        }

        fun include(vararg modules: Module) {
            includes.addAll(modules)
        }
    }

    fun module(name: String, code: Module.() -> Unit): Module {
        val module = modules.getOrPut(name) { Module(name) }
        module.code()
        return module
    }
}

fun configureMultiModuleMavenPublishing(code: MultiModuleMavenPublishingConfiguration.() -> Unit) {
    val publishingConfiguration = MultiModuleMavenPublishingConfiguration()
    publishingConfiguration.code()

    val components = publishingConfiguration
        .modules
        .mapValues { (_, module) -> project.createModulePublication(module) }

    val componentsWithExternals = publishingConfiguration
        .modules
        .filter { (_, module) -> module.includes.isNotEmpty() }
        .mapValues { (moduleName, module) ->
            val mainComponent = components[moduleName] ?: error("Component with name $moduleName wasn't created")
            val externalComponents = module.includes
                .map { components[it.name] ?: error("Component with name ${it.name} wasn't created") }
                .toSet()
            ComponentWithExternalVariants(mainComponent, externalComponents)
        }

    // override some components wih items from componentsWithExternals
    val mergedComponents = components + componentsWithExternals

    val publicationsContainer = publishing.publications
    for ((componentName, component) in mergedComponents) {
        publicationsContainer.create<MavenPublication>(componentName) {
            from(component)
            val module = publishingConfiguration.modules[componentName]!!
            module.mavenPublicationConfigurations.forEach { configure -> configure() }
        }
    }
}

fun Project.createModulePublication(module: MultiModuleMavenPublishingConfiguration.Module): AdhocComponentWithVariants {
    val softwareComponentFactory = (project as ProjectInternal).services.get(SoftwareComponentFactory::class.java)
    val component = softwareComponentFactory.adhoc(module.name)
    module.variants.values.forEach { addVariant(component, it) }

    return component
}

fun Project.addVariant(component: AdhocComponentWithVariants, variant: MultiModuleMavenPublishingConfiguration.Module.Variant) {
    val configuration = configurations.getOrCreate(variant.name)
    configuration.apply {
        isCanBeResolved = false
        isCanBeConsumed = true

        variant.attributesConfigurations.forEach { configure -> attributes.configure() }
    }

    for ((artifactNotation, configure) in variant.artifactsWithConfigurations) {
        artifacts.add(configuration.name, artifactNotation) {
            configure()
        }
    }

    for (configure in variant.configurationConfigurations) {
        configuration.apply(configure)
    }

    component.addVariantsFromConfiguration(configuration) {
        variant.variantDetailsConfigurations.forEach { configure -> configure() }
    }
}

private class ComponentWithExternalVariants(
    private val mainComponent: SoftwareComponent,
    private val externalComponents: Set<SoftwareComponent>
) : ComponentWithVariants, SoftwareComponentInternal {
    override fun getName(): String = mainComponent.name

    override fun getUsages(): Set<UsageContext> = (mainComponent as SoftwareComponentInternal).usages

    override fun getVariants(): Set<SoftwareComponent> = externalComponents
}