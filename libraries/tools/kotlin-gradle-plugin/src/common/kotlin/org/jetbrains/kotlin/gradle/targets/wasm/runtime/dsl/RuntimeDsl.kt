package org.jetbrains.kotlin.gradle.targets.wasm.runtime.dsl

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.wasm.runtime.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.gradle.targets.wasm.runtime.KotlinCommonSubTarget
import java.nio.file.Path


/**
 * Defines a new execution environment (runtime) for the wasmWasi target.
 *
 * This function registers a runtime with the given [name] and [version] and lets you
 * configure how it is obtained and used via the [configure] block. Inside the block
 * you can specify:
 * - where to [WasmWasiRuntimeDsl.download] the runtime from (per OS/Arch/Version),
 * - how to process downloaded artifacts via [WasmWasiRuntimeDsl.archiveOperation] and [WasmWasiRuntimeDsl.extractAction],
 * - how to resolve the runtime [WasmWasiRuntimeDsl.executable],
 * - additional arguments for running and testing via [WasmWasiRuntimeDsl.runArgs] and [WasmWasiRuntimeDsl.testArgs],
 * - optional file [WasmWasiRuntimeDsl.extension] and whether to [WasmWasiRuntimeDsl.allowInsecureProtocol].
 *
 * Returns the created sub-target representing this runtime so it can be further
 * customized if needed.
 */
@ExperimentalWasmDsl
fun KotlinWasmWasiTargetDsl.runtime(
    name: String,
    version: String,
    configure: WasmWasiRuntimeDsl.() -> Unit
): KotlinJsIrSubTarget {
    this as KotlinJsIrTarget
    return project.objects.newInstance(
        KotlinCommonSubTarget::class.java,
        this,
        name,
        version,
    ).also { subTarget: KotlinCommonSubTarget ->
        subTarget.configure()
        subTarget.subTargetConfigurators.add(CommonEnvironmentConfigurator(subTarget))
        val runtime = WasmWasiRuntimeImpl(subTarget)
        runtime.configure()

        subTarget.envSpec.extension.convention(runtime.extension)
        subTarget.envSpec.allowInsecureProtocol.convention(runtime.allowInsecureProtocol)

        subTargets.add(subTarget)
    }
}

@ExperimentalWasmDsl
/**
 * DSL used inside KotlinWasmWasiTargetDsl.runtime to describe how a custom WASI runtime
 * is discovered, downloaded, unpacked and invoked for a wasmWasi target.
 *
 * Use this DSL to:
 * - declare where to download runtime binaries for a given OS/arch/version via [download],
 * - transform downloaded archives with [archiveOperation] and perform extra extraction steps with [extractAction],
 * - resolve the runtime executable path with [executable],
 * - provide additional command-line arguments for run and test tasks with [runArgs] and [testArgs],
 * - optionally specify a runtime file [extension] and whether downloads may use insecure protocols via [allowInsecureProtocol].
 */
interface WasmWasiRuntimeDsl {
    /**
     * Optional file extension of the runtime artifact (for example, ".zip" or ".tar.gz").
     * If provided, it can be used by tooling to recognize how the runtime archive should be handled.
     */
    val extension: Property<String>

    /**
     * Whether HTTP (insecure) downloads are allowed when fetching the runtime.
     * Defaults to Gradle's global setting unless explicitly configured.
     */
    val allowInsecureProtocol: Property<Boolean>

    /**
     * Configure where the runtime should be downloaded from.
     * The lambda is invoked with resolved [os], [arch] and [version] and must return a base URL
     * (or a full URL) to fetch the runtime artifact for that platform.
     *
     * If this method is not called, the tool will not be downloaded.
     * In that case, subsequent hooks (such as [extractAction] and [executable]) will receive
     * `installationDir = null`.
     */
    fun download(
        configure: (
            os: String,
            arch: String,
            version: String
        ) -> String
    )

    /**
     * Customize how downloaded archives are processed. Use [archiveOperations] to access Gradle's
     * archive utilities and [entry] to refer to the downloaded file. The lambda should return
     * a value that represents the processed artifact (for example, a directory or file path).
     */
    fun archiveOperation(
        configure: (
            archiveOperations: ArchiveOperations,
            entry: Path,
        ) -> Any
    )

    /**
     * Provide an additional extraction or setup step after the archive is obtained.
     * It is invoked with [os], [arch], [version] and the target [installationDir].
     * Use this hook to unpack files, adjust permissions.
     *
     * Note: if [download] is not configured, `installationDir` will be `null`.
     */
    fun extractAction(
        configure: (
            os: String,
            arch: String,
            version: String,
            installationDir: Path?
        ) -> Unit
    )

    /**
     * Resolve the command or path to the runtime executable inside the prepared installation.
     * The lambda is invoked with [os], [arch], [version] and the [installationDir] and must
     * return the absolute or relative path to the executable to run.
     *
     * If a relative path is returned and `installationDir` is not null, it will be resolved
     * relative to `installationDir`.
     *
     * Note: if [download] is not configured, `installationDir` will be `null`.
     */
    fun executable(
        configure: (
            os: String,
            arch: String,
            version: String,
            installationDir: Path?
        ) -> String
    )

    /**
     * Command-line arguments to pass when running a wasm binary with this runtime.
     * The lambda receives the sandbox [isolationDir] and the wasm [entry] file and must return
     * a list of arguments to be appended to the runtime invocation.
     */
    fun runArgs(
        configure: (
            isolationDir: Path,
            entry: Path
        ) -> List<String>
    )

    /**
     * Command-line arguments to pass when executing tests with this runtime.
     * The lambda receives the sandbox [isolationDir] and the test [entry] file and must return
     * a list of arguments to be appended to the runtime invocation for tests.
     */
    fun testArgs(
        configure: (
            isolationDir: Path,
            entry: Path,
        ) -> List<String>
    )
}

@ExperimentalWasmDsl
internal class WasmWasiRuntimeImpl(
    private val subTarget: KotlinCommonSubTarget
) : WasmWasiRuntimeDsl {
    private val os: Provider<String> = subTarget.os
    private val arch: Provider<String> = subTarget.arch
    private val versionValue
        get() = subTarget.version

    override val extension: Property<String> = subTarget.project.objects.property(String::class.java)

    override val allowInsecureProtocol: Property<Boolean> = subTarget.project.objects.property(Boolean::class.java)

    override fun download(configure: (os: String, arch: String, version: String) -> String) {
        subTarget.envSpec.download.set(true)
        subTarget.envSpec.downloadBaseUrl.set(
            os.zip(arch) { osValue, archValue ->
                configure(osValue, archValue, versionValue)
            }
        )
    }

    override fun archiveOperation(
        configure: (
            archiveOperations: ArchiveOperations,
            entry: Path,
        ) -> Any
    ) {
        subTarget.archiveOperation.set(configure)
    }

    override fun extractAction(configure: (os: String, arch: String, version: String, installationDir: Path?) -> Unit) {
        subTarget.setupTask.configure {
            it.extractionAction.set(configure)
        }
    }

    override fun executable(configure: (os: String, arch: String, version: String, installationDir: Path?) -> String) {
        subTarget.envSpec.executableCommand.set(
            os.zip(arch) { osValue, archValue ->
                { dir: Path ->
                    configure(osValue, archValue, versionValue, dir)
                }
            }
        )
    }

    override fun runArgs(configure: (isolationDir: Path, entry: Path) -> List<String>) {
        subTarget.runArgs.set(configure)
    }

    override fun testArgs(configure: (isolationDir: Path, entry: Path) -> List<String>) {
        subTarget.testArgs.set(configure)
    }

}