#include "HotReload.hpp"

#include <cstdio>
#include <cstdint>
#include <cstring>
#include <optional>
#include <vector>

// Must be in synch with `BootstrapMetadata.kt`
extern "C" RUNTIME_WEAK const uint8_t* bootStartManifest;

enum class PayloadKind : uint8_t {
    kObject = 0,
    kArchive = 1,
};

struct ManifestEntryMetadata {
    PayloadKind kind;
    uint64_t offset;
    uint64_t size;
};

struct ManifestMetadata {
    uint64_t manifestSize;
    uint64_t bundleSize;
    std::vector<ManifestEntryMetadata> entries;
};

struct KaldoManifestReader {
    /**
    * Parses the content of [bootStartManifest] without copying any payload.
    * Integer fields use the target's byte order, so they can be read directly
    * on the host for which this launcher was compiled.
    */
    static std::optional<ManifestMetadata> parse(const uint8_t* ptr) {
        if (!ptr || std::memcmp(ptr, kFormatIdentifier, kFormatIdentifierSize) != 0) {
            return std::nullopt;
        }
        ptr += kFormatIdentifierSize;

        const uint64_t manifestSize = read<uint64_t>(ptr);
        const uint64_t bundleSize = read<uint64_t>(ptr);
        const uint32_t entryCount = read<uint32_t>(ptr);

        const uint64_t metadataSize = kHeaderSize + entryCount * kEntrySize;
        if (manifestSize < metadataSize || manifestSize - metadataSize != bundleSize) {
            return std::nullopt;
        }

        ManifestMetadata manifest{manifestSize, bundleSize, {}};
        manifest.entries.reserve(entryCount);

        for (uint32_t i = 0; i < entryCount; ++i) {
            const auto kind = static_cast<PayloadKind>(*ptr++);
            const uint64_t offset = read<uint64_t>(ptr);
            const uint64_t size = read<uint64_t>(ptr);

            if ((kind != PayloadKind::kObject && kind != PayloadKind::kArchive) ||
                    offset > bundleSize || size > bundleSize - offset) {
                return std::nullopt;
            }
            manifest.entries.push_back({kind, offset, size});
        }

        return manifest;
    }

private:
    static constexpr char kFormatIdentifier[] = "KALD0";
    static constexpr uint64_t kFormatIdentifierSize = sizeof(kFormatIdentifier) - 1;
    static constexpr uint64_t kHeaderSize =
            kFormatIdentifierSize + sizeof(uint64_t) + sizeof(uint64_t) + sizeof(uint32_t);
    static constexpr uint64_t kEntrySize = sizeof(uint8_t) + sizeof(uint64_t) + sizeof(uint64_t);

    template <typename T>
    static T read(const uint8_t*& ptr) {
        T value;
        std::memcpy(&value, ptr, sizeof(value));
        ptr += sizeof(value);
        return value;
    }
};

// TODO(Gabriele): This function will be renamed into future. Right now, it does nothing
// TODO(Gabriele): since the hot-reload runtime is not implemented.
extern "C" RUNTIME_EXPORT void* KNHR_LoadObjCStubAddress(void* arg) {
    return nullptr;
}

extern "C" RUNTIME_EXPORT int Konan_main(const int argc, const char** argv) {

    Kotlin_initRuntimeIfNeeded();

    fprintf(stderr,
        "[warning] :: hot-reload runtime is not implemented yet, thus this program does nothing "
        "and will now terminate. Please use the 'closed' compilation scheme instead.\n");

    auto manifest = KaldoManifestReader::parse(bootStartManifest);
    if (!manifest) {
        fprintf(stderr, "[error] :: invalid bootstrap manifest\n");
        Kotlin_shutdownRuntime();
        return 1;
    }
    for (const auto& entry : manifest->entries) {
        fprintf(stderr, "[debug] :: embedded payload kind=%u offset=%llu size=%llu\n",
                static_cast<unsigned>(entry.kind),
                static_cast<unsigned long long>(entry.offset),
                static_cast<unsigned long long>(entry.size));
    }

    Kotlin_shutdownRuntime();

    return 0;
}