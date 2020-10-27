package org.jetbrains.kotlin.backend.konan

object BitcodeEmbedding {

    enum class Mode {
        NONE, MARKER, FULL
    }

    internal fun getLinkerOptions(config: KonanConfig): List<String> = when (config.bitcodeEmbeddingMode) {
        Mode.NONE -> emptyList()
        Mode.MARKER -> listOf("-bitcode_bundle", "-bitcode_process_mode", "marker")
        Mode.FULL -> listOf("-bitcode_bundle")
    }

    internal fun getClangOptions(config: KonanConfig): List<String> = when (config.bitcodeEmbeddingMode) {
        Mode.NONE -> listOf("-fembed-bitcode=off")
        Mode.MARKER -> listOf("-fembed-bitcode=marker")
        Mode.FULL -> listOf("-fembed-bitcode=all")
    }

    private val KonanConfig.bitcodeEmbeddingMode get() = configuration.get(KonanConfigKeys.BITCODE_EMBEDDING_MODE)!!
}