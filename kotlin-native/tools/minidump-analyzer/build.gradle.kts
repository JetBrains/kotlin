import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.tools.ToolExecutionTask

plugins {
    id("native")
    id("jvm-toolchains")
}

val executable = "minidump-analyzer.exe"
val sourcesToBuildExecutable = listOf(
        // This list is taken from src/tools/mac/dump_syms/dump_syms.xcodeproj in Breakpad
        "src/common/mac/arch_utilities.cc",
        "src/common/mac/macho_reader.cc",
        "src/common/dwarf/elf_reader.cc",
        "src/common/dwarf/dwarf2reader.cc",
        "src/common/path_helper.cc",
        "src/common/dwarf/bytereader.cc",
        "src/common/mac/macho_utilities.cc",
        "src/common/mac/file_id.cc",
        "src/common/mac/macho_id.cc",
        "src/common/mac/macho_walker.cc",
        "src/common/mac/dump_syms.cc",
        "src/common/dwarf/dwarf2diehandler.cc",
        "src/common/dwarf_cu_to_module.cc",
        "src/common/dwarf_line_to_module.cc",
        "src/common/dwarf_range_list_handler.cc",
        "src/common/language.cc",
        "src/common/module.cc",
        "src/common/dwarf_cfi_to_module.cc",
        "src/common/stabs_reader.cc",
        "src/common/stabs_to_module.cc",
        "src/common/md5.cc",
        // This list is taken from running `make src/processor/minidump_stackwalk` in Breakpad
        "src/common/path_helper.cc",
        "src/processor/basic_code_modules.cc",
        "src/processor/basic_source_line_resolver.cc",
        "src/processor/call_stack.cc",
        "src/processor/cfi_frame_info.cc",
        "src/processor/convert_old_arm64_context.cc",
        "src/processor/disassembler_x86.cc",
        "src/processor/dump_context.cc",
        "src/processor/dump_object.cc",
        "src/processor/exploitability.cc",
        "src/processor/exploitability_linux.cc",
        "src/processor/exploitability_win.cc",
        "src/processor/logging.cc",
        "src/processor/minidump.cc",
        "src/processor/minidump_processor.cc",
        "src/processor/pathname_stripper.cc",
        "src/processor/process_state.cc",
        "src/processor/proc_maps_linux.cc",
        "src/processor/simple_symbol_supplier.cc",
        "src/processor/source_line_resolver_base.cc",
        "src/processor/stack_frame_cpu.cc",
        "src/processor/stack_frame_symbolizer.cc",
        "src/processor/stackwalk_common.cc",
        "src/processor/stackwalker.cc",
        "src/processor/stackwalker_address_list.cc",
        "src/processor/stackwalker_amd64.cc",
        "src/processor/stackwalker_arm.cc",
        "src/processor/stackwalker_arm64.cc",
        "src/processor/stackwalker_mips.cc",
        "src/processor/stackwalker_ppc.cc",
        "src/processor/stackwalker_ppc64.cc",
        "src/processor/stackwalker_riscv.cc",
        "src/processor/stackwalker_riscv64.cc",
        "src/processor/stackwalker_sparc.cc",
        "src/processor/stackwalker_x86.cc",
        "src/processor/symbolic_constants_win.cc",
        "src/processor/tokenize.cc",
        "src/third_party/libdisasm/ia32_implicit.c",
        "src/third_party/libdisasm/ia32_insn.c",
        "src/third_party/libdisasm/ia32_invariant.c",
        "src/third_party/libdisasm/ia32_modrm.c",
        "src/third_party/libdisasm/ia32_opcode_tables.c",
        "src/third_party/libdisasm/ia32_operand.c",
        "src/third_party/libdisasm/ia32_reg.c",
        "src/third_party/libdisasm/ia32_settings.c",
        "src/third_party/libdisasm/x86_disasm.c",
        "src/third_party/libdisasm/x86_format.c",
        "src/third_party/libdisasm/x86_imm.c",
        "src/third_party/libdisasm/x86_insn.c",
        "src/third_party/libdisasm/x86_misc.c",
        "src/third_party/libdisasm/x86_operand_list.c",
)

val breakpadRoot by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("sources-directory"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

dependencies {
    breakpadRoot(project(":kotlin-native:runtime", "breakpadSources"))
}

val breakpadRootAsFile = breakpadRoot.incoming.files.singleFile

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    suffixes {
        (".cc" to ".$obj") {
            tool(*hostPlatform.clang.clangCXX("").toTypedArray())
            flags(
                    "-I$breakpadRootAsFile/src",
                    "-I${projectDir.resolve("src/main/include")}",
                    "-DHAVE_MACH_O_NLIST_H",
                    "-DHAVE_CONFIG_H",
                    "-DBP_LOGGING_INCLUDE=\"loggingDisabled.h\"",
                    "-DBPLOG_INFO=LoggingLevelDisabled()",
                    *reproducibilityCompilerFlags,
                    "-c", "-o", ruleOut(), ruleInFirst())
        }
        (".c" to ".$obj") {
            tool(*hostPlatform.clang.clangC("").toTypedArray())
            flags(
                    *reproducibilityCompilerFlags,
                    "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            collection.builtBy(breakpadRoot)
            sourcesToBuildExecutable.forEach {
                collection.from(breakpadRootAsFile.resolve(it))
            }
            dir("src/main/cpp")
        }
    }
    val objSetCpp = sourceSets["main"]!!.transform(".cc" to ".$obj")
    val objSetC = sourceSets["main"]!!.transform(".c" to ".$obj")

    target(executable, objSetCpp, objSetC) {
        tool(*hostPlatform.clang.clangCXX("").toTypedArray())
        flags("-o", ruleOut(), *ruleInAll())
    }
}

val nativeExecutable by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("native-executable"))
    }
}

artifacts {
    add(nativeExecutable.name, tasks.named<ToolExecutionTask>(executable).map { it.output })
}