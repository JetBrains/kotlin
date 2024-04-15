#!/usr/bin/python3
#
# Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license

import argparse
import hashlib
import os
import shlex
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path
from typing import List

vsdevcmd = None
isysroot = None

ninja = 'ninja'
cmake = 'cmake'
git = 'git'


def absolute_path(path):
    if path is not None:
        # CMake is not tolerant to backslashes in path.
        return os.path.abspath(path).replace('\\', '/')
    else:
        return None


def host_is_windows():
    return sys.platform == "win32"


def host_is_linux():
    return sys.platform == "linux"


def host_is_darwin():
    return sys.platform == "darwin"


def host_llvm_target():
    return "Native"


def host_default_compression():
    """
    Determine archive compression method based on current OS.
    On Windows we use `zip` and `tar.gz` otherwise.
    """
    if host_is_windows():
        return "zip"
    else:
        return "gztar"


def detect_xcode_sdk_path():
    """
    Get an absolute path to macOS SDK.
    """
    return subprocess.check_output(['xcrun', '--show-sdk-path'],
                                   universal_newlines=True).rstrip()


def detect_vsdevcmd():
    """
    Use vswhere (and download it, if needed) utility to find path to vsdevcmd.bat.
    :return: path to vsdevcmd.bat
    """
    vswhere = shutil.which('vswhere')
    if vswhere is None:
        print("Downloading vswhere utility to detect path to vsdevcmd.bat automatically")
        vswhere_url = "https://github.com/microsoft/vswhere/releases/download/2.8.4/vswhere.exe"
        urllib.request.urlretrieve(vswhere_url, 'vswhere.exe')
        vswhere = shutil.which('vswhere')
        if vswhere is None:
            sys.exit("Failed to retrieve vswhere utility. Please provide path to vsdevcmd.bat with --vsdevcmd")
    vswhere_args = [vswhere, '-prerelease', '-latest', '-property', 'installationPath']
    path_to_visual_studio = subprocess.check_output(vswhere_args, universal_newlines=True).rstrip()
    vsdevcmd_path = os.path.join(path_to_visual_studio, "Common7", "Tools", "vsdevcmd.bat")
    if not os.path.isfile(vsdevcmd_path):
        sys.exit("vsdevcmd.bat is not found. Please provide path to vsdevcmd.bat with --vsdevcmd")
    else:
        print("Found vsdevcmd.bat: " + vsdevcmd_path)
    return vsdevcmd_path


def construct_cmake_flags(
        bootstrap_llvm_path: str = None,
        install_path: str = None,
        projects: List[str] = None,
        runtimes: List[str] = None,
        targets: List[str] = None,
        distribution_components: List[str] = None
) -> List[str]:
    building_bootstrap = bootstrap_llvm_path is None

    c_compiler, cxx_compiler, linker, ar = None, None, None, None
    c_flags, cxx_flags, linker_flags = None, None, None

    cmake_args = [
        '-DCMAKE_BUILD_TYPE=Release',
        '-DLLVM_ENABLE_ASSERTIONS=OFF',
        '-DLLVM_ENABLE_TERMINFO=OFF',
        '-DLLVM_INCLUDE_GO_TESTS=OFF',
        '-DLLVM_ENABLE_Z3_SOLVER=OFF',
        '-DCOMPILER_RT_BUILD_BUILTINS=ON',
        '-DLLVM_ENABLE_THREADS=ON',
        '-DLLVM_OPTIMIZED_TABLEGEN=ON',
        '-DLLVM_ENABLE_IDE=OFF',
        '-DLLVM_BUILD_UTILS=ON',
        '-DLLVM_INSTALL_UTILS=ON'
    ]
    if not building_bootstrap:
        if distribution_components:
            cmake_args.append('-DLLVM_DISTRIBUTION_COMPONENTS=' + ';'.join(distribution_components))
            # These links are actually copies on windows, so they're wasting precious disk space.
            cmake_args.append("-DCLANG_LINKS_TO_CREATE=clang++")
            cmake_args.append("-DLLD_SYMLINKS_TO_CREATE=ld.lld;wasm-ld")

        if host_is_windows():
            # CMake is not tolerant to backslashes
            c_compiler = f'{bootstrap_llvm_path}/bin/clang-cl.exe'.replace('\\', '/')
            cxx_compiler = f'{bootstrap_llvm_path}/bin/clang-cl.exe'.replace('\\', '/')
            linker = f'{bootstrap_llvm_path}/bin/lld-link.exe'.replace('\\', '/')
            ar = f'{bootstrap_llvm_path}/bin/llvm-lib.exe'.replace('\\', '/')
        elif host_is_linux():
            c_compiler = f'{bootstrap_llvm_path}/bin/clang'
            cxx_compiler = f'{bootstrap_llvm_path}/bin/clang++'
            linker = f'{bootstrap_llvm_path}/bin/ld.lld'
            ar = f'{bootstrap_llvm_path}/bin/llvm-ar'
        elif host_is_darwin():
            c_compiler = f'{bootstrap_llvm_path}/bin/clang'
            cxx_compiler = f'{bootstrap_llvm_path}/bin/clang++'
            # ld64.lld is not that good yet.
            linker = None
            ar = f'{bootstrap_llvm_path}/bin/llvm-ar'
            c_flags = ['-isysroot', isysroot]
            cxx_flags = ['-isysroot', isysroot, '-stdlib=libc++']
            linker_flags = ['-stdlib=libc++']

    if host_is_darwin():
        cmake_args.append('-DLLVM_ENABLE_LIBCXX=ON')
        if building_bootstrap:
            # Don't waste time by doing unnecessary work for throwaway toolchain.
            cmake_args.extend([
                '-DCOMPILER_RT_BUILD_CRT=OFF',
                '-DCOMPILER_RT_BUILD_LIBFUZZER=OFF',
                '-DCOMPILER_RT_BUILD_SANITIZERS=OFF',
                '-DCOMPILER_RT_BUILD_XRAY=OFF',
                '-DCOMPILER_RT_ENABLE_IOS=OFF',
                '-DCOMPILER_RT_ENABLE_WATCHOS=OFF',
                '-DCOMPILER_RT_ENABLE_TVOS=OFF',
            ])
        else:
            cmake_args.append('-DLIBCXX_USE_COMPILER_RT=ON')

    if install_path is not None:
        cmake_args.append('-DCMAKE_INSTALL_PREFIX=' + install_path)
    if targets is not None:
        cmake_args.append('-DLLVM_TARGETS_TO_BUILD=' + ";".join(targets))
    if projects is not None:
        cmake_args.append('-DLLVM_ENABLE_PROJECTS=' + ";".join(projects))
    if runtimes is not None:
        cmake_args.append('-DLLVM_ENABLE_RUNTIMES=' + ";".join(runtimes))
    if c_compiler is not None:
        cmake_args.append('-DCMAKE_C_COMPILER=' + c_compiler)
    if cxx_compiler is not None:
        cmake_args.append('-DCMAKE_CXX_COMPILER=' + cxx_compiler)
    if linker is not None:
        cmake_args.append('-DCMAKE_LINKER=' + linker)
    if ar is not None:
        cmake_args.append('-DCMAKE_AR=' + ar)

    if c_flags is not None:
        cmake_args.append("-DCMAKE_C_FLAGS=" + ' '.join(c_flags))
    if cxx_flags is not None:
        cmake_args.append("-DCMAKE_CXX_FLAGS=" + ' '.join(cxx_flags))
    if linker_flags is not None:
        cmake_args.append('-DCMAKE_EXE_LINKER_FLAGS=' + ' '.join(linker_flags))
        cmake_args.append('-DCMAKE_MODULE_LINKER_FLAGS=' + ' '.join(linker_flags))
        cmake_args.append('-DCMAKE_SHARED_LINKER_FLAGS=' + ' '.join(linker_flags))

    if host_is_windows():
        # Use MT to make distribution self-contained
        # TODO: Consider -DCMAKE_INSTALL_UCRT_LIBRARIES=ON as an alternative
        cmake_args.append('-DLLVM_USE_CRT_RELEASE=MT')
        cmake_args.append('-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded')
        # We don't support PDB, so no need fir DIA.
        cmake_args.append('-DLLVM_ENABLE_DIA_SDK=OFF')

    # Make distribution much smaller by linking to dynamic library
    # instead of static linkage.
    # Not working for Windows yet.
    #
    # Also not working for Linux and macOS because of signal chaining.
    # TODO: Enable after LLVM distribution patching.
    if not host_is_windows():
        cmake_args.append("-DLLVM_BUILD_LLVM_DYLIB=OFF")
        cmake_args.append("-DLLVM_LINK_LLVM_DYLIB=OFF")

    return cmake_args


def run_command(command: List[str], dry_run):
    """
    Execute single command in terminal/cmd.

    Note that on Windows we prepare environment with vsdevcmd.bat.
    """
    if host_is_windows():
        if vsdevcmd is None:
            sys.exit("'VsDevCmd.bat' is not set!")
        command = [vsdevcmd, "-arch=amd64", "&&"] + command
        print("Running command: " + ' '.join(command))
    else:
        command = [shlex.quote(arg) for arg in command]
        command = ' '.join(command)
        print("Running command: " + command)

    if not dry_run:
        subprocess.run(command, shell=True, check=True)
        
def force_create_directory(parent, name) -> Path:
    build_path = parent / name
    print(f"Force-creating directory {build_path}")
    if build_path.exists():
        shutil.rmtree(build_path)
    os.mkdir(build_path)
    return build_path


def llvm_build_commands(
        install_path, bootstrap_path, llvm_src, targets, build_targets, projects, runtimes, distribution_components, debug_cmake
) -> List[List[str]]:
    cmake_flags = construct_cmake_flags(bootstrap_path, install_path, projects, runtimes, targets, distribution_components)

    debug_cmake_flag = ["--debug-trycompile"] if debug_cmake else []
    cmake_command = [cmake, "-G", "Ninja"] + debug_cmake_flag + cmake_flags + [os.path.join(llvm_src, "llvm")]
    ninja_command = [ninja] + build_targets
    return [cmake_command, ninja_command]


def clone_llvm_repository(repo, branch, llvm_repo_destination, dry_run):
    """
    Downloads a single commit from the given repository.
    """
    if host_is_darwin():
        default_repo, default_branch = "https://github.com/apple/llvm-project", "apple/stable/20200714"
    else:
        default_repo, default_branch = "https://github.com/llvm/llvm-project", "release/11.x"
    repo = default_repo if repo is None else repo
    branch = default_branch if branch is None else branch
    # Download only single commit because we don't need whole history just for building LLVM.
    run_command([git, "clone", repo, "--branch", branch, "--depth", "1", "llvm-project"], dry_run)
    return absolute_path(llvm_repo_destination)


def default_num_stages():
    # Perform bootstrap build
    return 2


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Build LLVM toolchain for Kotlin/Native")
    # Output configuration.
    parser.add_argument("--install-path", type=str, default="llvm-distribution", required=False,
                        help="Where final LLVM distribution will be installed")
    parser.add_argument("--pack", action='store_true',
                        help="Create an archive and its sha256 for final distribution at `--install-path`")
    parser.add_argument("--build-targets", default=["install"],
                        nargs="+",
                        help="What components should be installed")
    parser.add_argument("--distribution-components", default=None,
                        nargs="+",
                        help="What components should be installed with `install-distribution` target")
    # Build configuration
    parser.add_argument("--stage0", type=str, default=None,
                        help="Path to existing LLVM toolchain")
    parser.add_argument("--num-stages", type=int, default=default_num_stages(),
                        help="Number of stages in bootstrap.")
    # LLVM sources.
    parser.add_argument("--llvm-sources", dest="llvm_src", type=str, default=None,
                        help="Location of LLVM sources")
    parser.add_argument("--repo", type=str, default=None)
    parser.add_argument("--branch", type=str, default=None)
    parser.add_argument("--llvm-repo-destination", type=str, default="llvm-project",
                        help="Where LLVM repository should be downloaded.")
    # Environment setup.
    parser.add_argument("--vsdevcmd", type=str, default=None,
                        help="(Windows only) Path to VsDevCmd.bat")
    parser.add_argument("--ninja", type=str, default=None,
                        help="Override path to ninja")
    parser.add_argument("--cmake", type=str, default=None,
                        help="Override path to cmake")
    parser.add_argument("--git", type=str, default=None,
                        help="Override path to git")
    parser.add_argument("--isysroot", type=str, default=None,
                        help="(macOS only) Override path to macOS SDK")
    parser.add_argument("--debug-cmake", action='store_true',
                        help="Add --debug-trycompile flag to cmake to save temporary CMakeError.log")
    # Misc.
    parser.add_argument("--save-temporary-files", action='store_true',
                        help="Should intermediate build results be saved?")
    parser.add_argument("--dry-run", action='store_true', help="Only print commands, do not run")
    return parser


def build_distribution(args):
    """
    Performs (probably multistage) build of LLVM
    and returns path to the final distribution.
    """
    current_dir = Path().absolute()
    num_stages = args.num_stages
    bootstrap_path = args.stage0
    intermediate_build_results = []
    # Most likely, num_stages will be 1 or 2.
    # 2 means bootstrap build: we build LLVM distribution (stage 1)
    # that then compiles sources once again (stage 2). Thus, resulting
    # distribution is (almost) independent from environment, which means
    # reproducibility and less bugs.
    #
    # Sometimes it makes sense to generate yet another distribution to check
    # that it is the same as built at stage 2 (so there is no non-determinism in LLVM).
    for stage in range(1, num_stages + 1):
        building_bootstrap = num_stages > 1 and stage == 1
        building_final = stage == num_stages

        if building_bootstrap:
            # We only need a host target to start a bootstrap.
            targets = [host_llvm_target()]
        else:
            # None targets means all available targets.
            targets = None
        if building_final:
            install_path = args.install_path
            build_targets = args.build_targets
        else:
            install_path = force_create_directory(current_dir, f"llvm-stage-{stage}")
            intermediate_build_results.append(install_path)
            build_targets = ["install"]

        projects = ["clang", "lld", "libcxx", "libcxxabi", "compiler-rt"]
        runtimes = None

        build_dir = force_create_directory(current_dir, f"llvm-stage-{stage}-build")
        intermediate_build_results.append(build_dir)
        commands = llvm_build_commands(
            install_path=absolute_path(install_path),
            bootstrap_path=absolute_path(bootstrap_path),
            llvm_src=absolute_path(args.llvm_src),
            targets=targets,
            build_targets=build_targets,
            projects=projects,
            runtimes=runtimes,
            distribution_components=args.distribution_components,
            debug_cmake=args.debug_cmake,
        )

        os.chdir(build_dir)
        for command in commands:
            run_command(command, args.dry_run)
        os.chdir(current_dir)
        bootstrap_path = install_path

    if not args.save_temporary_files:
        for dir in intermediate_build_results:
            print(f"Removing temporary directory: {dir}")
            shutil.rmtree(dir)

    return absolute_path(args.install_path)


def create_archive(input_directory, output_path, compression=host_default_compression()) -> str:
    print("Creating archive " + output_path + " from " + input_directory)
    base_directory, archive_prefix = os.path.split(os.path.normpath(input_directory))
    return shutil.make_archive(output_path, compression, base_directory, archive_prefix)


def create_checksum_file(input_path, output_path):
    chunk_size = 4096
    checksum = hashlib.sha256()
    with open(input_path, "rb") as input_contents:
        for chunk in iter(lambda: input_contents.read(chunk_size), b""):
            checksum.update(chunk)
    print(checksum.hexdigest(), file=open(output_path, "w"))
    return True


def setup_environment(args):
    """
    Setup globals that store information about script execution environment.
    """
    global vsdevcmd, ninja, cmake, git, isysroot
    # TODO: We probably can download some of these binaries ourselves.
    if args.ninja:
        ninja = args.ninja
    elif shutil.which('ninja') is None:
        sys.exit("'ninja' is not found. Install or provide via --ninja argument.")
    if args.cmake:
        cmake = args.cmake
    elif shutil.which('cmake') is None:
        sys.exit("'cmake' is not found. Install or provide via --cmake argument.")
    if args.git:
        git = args.git
    elif shutil.which('git') is None:
        sys.exit("'git' is not found. Install or provide via --git argument.")
    if host_is_windows():
        if args.vsdevcmd:
            vsdevcmd = args.vsdevcmd
        else:
            vsdevcmd = detect_vsdevcmd()
    elif host_is_darwin():
        if args.isysroot:
            isysroot = args.isysroot
        else:
            isysroot = detect_xcode_sdk_path()


def main():
    parser = build_parser()
    args = parser.parse_args()
    setup_environment(args)
    temporary_llvm_repo = None
    if args.llvm_src is None:
        temporary_llvm_repo = clone_llvm_repository(args.repo, args.branch, args.llvm_repo_destination, args.dry_run)
        args.llvm_src = temporary_llvm_repo
    final_dist = build_distribution(args)
    if args.pack:
        archive = create_archive(final_dist, args.install_path)
        create_checksum_file(archive, f"{archive}.sha256")
    if not args.save_temporary_files and temporary_llvm_repo is not None and not args.dry_run:
        print(f"Removing temporary directory: {temporary_llvm_repo}")
        shutil.rmtree(temporary_llvm_repo)


if __name__ == "__main__":
    main()
