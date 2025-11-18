//
// Created by Gabriele.Pappalardo on 11/11/2025.
//

#ifndef DYNAMICLINKERREGISTRY_HPP
#define DYNAMICLINKERREGISTRY_HPP

#include <vector>
#include <mach-o/dyld.h>
#include <mach-o/loader.h>
#include <mach-o/nlist.h>

#define MANGLED_FUN_NAME_PREFIX "_kfun:"
#define MANGLED_CLASS_NAME_PREFIX "_kclass:"

struct KotlinDynamicLibrary {
    std::string dylibPath;
    std::vector<std::string_view> functions{};
    std::vector<std::string_view> classes{};
};

static bool startsWith(const std::string_view str, const std::string_view prefix) {
    return str.size() >= prefix.size() && str.substr(0, prefix.size()) == prefix;
}

namespace dyld {
/**
 * Finds the LC_SYMTAB load command.
 */
inline const symtab_command* findSymtabCommand(const mach_header_64* header) {
    const auto cmdsStartAddress = reinterpret_cast<const uint8_t*>(header) + sizeof(mach_header_64);
    auto cmd = reinterpret_cast<const load_command*>(cmdsStartAddress);

    for (uint32_t i = 0; i < header->ncmds; ++i) {
        if (cmd->cmd == LC_SYMTAB) {
            return reinterpret_cast<const symtab_command*>(cmd);
        }
        cmd = reinterpret_cast<const load_command*>(reinterpret_cast<const uint8_t*>(cmd) + cmd->cmdsize);
    }
    return nullptr;
}

/**
 * Finds the __LINKEDIT segment to calculate the slide.
 */
inline const segment_command_64* findLinkeditSegment(const mach_header_64* header) {
    const auto cmdsStartAddress = reinterpret_cast<const uint8_t*>(header) + sizeof(mach_header_64);
    auto cmd = reinterpret_cast<const load_command*>(cmdsStartAddress);

    for (uint32_t i = 0; i < header->ncmds; ++i) {
        if (cmd->cmd == LC_SEGMENT_64) {
            auto seg = reinterpret_cast<const segment_command_64*>(cmd);
            if (strncmp(seg->segname, SEG_LINKEDIT, sizeof(SEG_LINKEDIT)) == 0) {
                return seg;
            }
        }
        cmd = reinterpret_cast<const load_command*>(reinterpret_cast<const uint8_t*>(cmd) + cmd->cmdsize);
    }
    return nullptr;
}

inline uint32_t findImageIndexOf(const std::string& lookedImageName) {
    const auto imageCount = _dyld_image_count();
    for (uint32_t imageIndex = 0; imageIndex < imageCount; ++imageIndex) {
        const auto imageName = _dyld_get_image_name(imageIndex);
        if (!imageName) continue;

        const std::string_view reportedName(imageName);
        // First, try for a quick exact match.
        if (reportedName == lookedImageName) return imageIndex;

        // Handle cases like /tmp vs /private/tmp by checking for a substring match.
        // This looks for "tmp/hot-test..." inside "/private/tmp/hot-test..."
        if (startsWith(lookedImageName, "/") && reportedName.find(lookedImageName.substr(1)) != std::string_view::npos) {
            return imageIndex;
        }
    }
    return 0;
}

inline std::string_view getSymbolNameFromStringTable(const char* stringTab, const uint32_t stringOffset) {
    return std::string_view{stringTab + stringOffset};
}

inline const char* getImageName(const uint32_t index) {
    return _dyld_get_image_name(index);
}

/// The dynamic library needs to be loaded first with dlopen, otherwise parsing will fail.
inline KotlinDynamicLibrary parseDynamicLibrary(const std::string& dylibPath) {
    const auto imageIndex = dyld::findImageIndexOf(dylibPath);
    if (imageIndex == 0) throw std::runtime_error("error: Could not find image: " + dylibPath);

    const auto imageName = dyld::getImageName(imageIndex);
    const auto imageBase = reinterpret_cast<const mach_header_64*>(_dyld_get_image_header(imageIndex));

    const auto slide = _dyld_get_image_vmaddr_slide(imageIndex);

    const auto symtabCmd = dyld::findSymtabCommand(imageBase);
    const auto linkedit = dyld::findLinkeditSegment(imageBase);

    if (!symtabCmd || !linkedit) {
        std::cerr << "error: Could not find symbol table or LINKEDIT in image: " << imageName << std::endl;
        return {};
    }

    // Calculate the in-memory addresses
    const uintptr_t linkeditBase = slide + linkedit->vmaddr - linkedit->fileoff;
    const auto symtab = reinterpret_cast<const nlist_64*>(linkeditBase + symtabCmd->symoff);
    const auto stringtab = reinterpret_cast<const char*>(linkeditBase + symtabCmd->stroff);

    std::vector<std::string_view> functions{};
    std::vector<std::string_view> classes{};

    for (uint32_t i = 0; i < symtabCmd->nsyms; ++i) {
        const nlist_64& sym = symtab[i];
        // Ignore undefined symbols, we can't reload them
        if ((sym.n_type & N_TYPE) == N_UNDF && (sym.n_type & N_EXT)) continue;
        const auto symbolName = dyld::getSymbolNameFromStringTable(stringtab, sym.n_un.n_strx);
        // Perform filtering based on how the name stars
        if (startsWith(symbolName, MANGLED_FUN_NAME_PREFIX)) {
            functions.push_back(symbolName.substr(1));
        } else if (startsWith(symbolName, MANGLED_CLASS_NAME_PREFIX)) {
            classes.push_back(symbolName.substr(1));
        }
    }

    return {dylibPath, functions, classes};
}

} // namespace dyld

#endif // DYNAMICLINKERREGISTRY_HPP
