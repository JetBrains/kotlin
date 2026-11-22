#!/usr/bin/env python3
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license.

import argparse
import ctypes
import json
import os
import platform
import plistlib
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import package as llvm_package


# newer xcode's libc++ versions reject parts of LLVM 21.1.6's sources.
DEFAULT_LLVM_XCODE_VERSION = "26.4"
# This workflow builds Kotlin's LLVM 21 macOS arm64 distribution.
LLVM_ARTIFACT_PREFIX = "llvm-21-aarch64-macos"
KOTLIN_REPO = Path(__file__).resolve().parents[3]


LOCAL_PLUGINS = {
    "kotlin-compose-compiler-plugin-embeddable": (
        ":plugins:compose-compiler-plugin:compiler:runtimeJar",
        "plugins/compose/compiler/build/libs/compiler-*.jar",
    ),
    "kotlin-serialization-compiler-plugin-embeddable": (
        ":kotlinx-serialization-compiler-plugin.embeddable:embeddable",
        "plugins/kotlinx-serialization/kotlinx-serialization.embeddable/build/libs/*.jar",
    ),
    "kotlin-atomicfu-compiler-plugin-embeddable": (
        ":kotlin-atomicfu-compiler-plugin-embeddable:embeddable",
        "plugins/atomicfu/atomicfu-compiler-embeddable/build/libs/*.jar",
    ),
}

TRAINING_PLUGIN_INIT = r'''import groovy.json.JsonOutput

// Keep the project's KGP; replace only these Native compiler plugin JARs.
def replacements = @PLUGIN_REPLACEMENTS@
gradle.taskGraph.whenReady { graph ->
    graph.allTasks.each { task ->
        if (!task.class.name.contains('KotlinNativeCompile') && !task.class.name.contains('KotlinNativeLink')) return
        if (task.class.name.contains('KotlinNativeLink')) {
            // Final native link is the main PGO optimization target, make sure it runs.
            task.outputs.upToDateWhen { false }
            task.outputs.cacheIf { false }
        }
        def remap = { files -> task.project.files({ files.files.collect { file ->
            def key = replacements.keySet().find { file.name.startsWith(it + '-') }
            key == null ? file : new File(replacements[key])
        } }) }
        if (task.compilerPluginClasspath != null) task.compilerPluginClasspath = remap(task.compilerPluginClasspath)
        def original = task.kotlinPluginData
        if (original != null) task.kotlinPluginData = original.map { data ->
            data.copy(remap(data.classpath), data.options, data.inputsOutputsState)
        }
        task.doFirst {
            def files = []
            if (task.compilerPluginClasspath != null) files += task.compilerPluginClasspath.files
            if (task.kotlinPluginData != null && task.kotlinPluginData.present) files += task.kotlinPluginData.get().classpath.files
            println('TRAINING_NATIVE_PLUGINS ' + JsonOutput.toJson([task: task.path, files: files*.absolutePath]))
        }
    }
}
'''


def build_parser():
    parser = argparse.ArgumentParser(
        description="Build and package LLVM with local compiler plugins on macOS arm64.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument("--training-repo", required=True, help="Git remote of the training project")
    parser.add_argument("--training-branch", required=True, help="Branch or tag to clone")
    parser.add_argument("--training-command", required=True, help="Shell command to prepare and compile the training project")
    parser.add_argument("--output-dir", type=Path, default=Path("llvm-pgo-distribution"), help="Directory containing the generated distributions and profile")
    parser.add_argument("--version-number", type=int, required=True, help="Kotlin/Native LLVM artifact revision number")
    parser.add_argument(
        "--kotlin-native-target",
        help="Kotlin/Native target for platform libraries, e.g. ios_arm64 for a physical iOS device or ios_simulator_arm64 for an arm64 simulator; by default all targets are built",
    )
    parser.add_argument("--stage0", type=Path, help="Existing LLVM bootstrap; otherwise build one")
    parser.add_argument("--llvm-sources", type=Path, help="Existing LLVM checkout; otherwise use package.py's default")
    parser.add_argument(
        "--llvm-xcode-version", default=DEFAULT_LLVM_XCODE_VERSION,
        help="Xcode version used for the LLVM builds; its macOS SDK must be compatible with the LLVM sources",
    )
    parser.add_argument(
        "--distribution-components", nargs="+",
        default=["clang", "libclang", "lld", "llvm-cov", "llvm-profdata", "llvm-ar", "clang-resource-headers"],
        help="Essentials distribution components; the dev distribution always uses the full install",
    )
    parser.add_argument("--save-temporary-files", action="store_true", help="Keep intermediate builds, training project and profiles")
    return parser


def run(command, cwd=None, env=None):
    print("Running:", command, flush=True)
    subprocess.run(command, cwd=cwd, env={**os.environ, **(env or {})}, check=True)


def artifact_names(version_number):
    return (
        f"{LLVM_ARTIFACT_PREFIX}-dev-{version_number}",
        f"{LLVM_ARTIFACT_PREFIX}-essentials-{version_number}",
    )


def wait_for_gradle_daemons(gradle_home, timeout=120):
    """Wait for this isolated home's single-use JVMs to flush native profiles."""
    pids = {int(log.name.split("-")[1].split(".")[0]) for log in (gradle_home / "daemon").glob("*/daemon-*.out.log")}
    marker = f" KONAN_PGO_BUILD_PROCESS_MARKER={gradle_home} "
    deadline = time.monotonic() + timeout
    while pids:
        result = subprocess.run(
            ["ps", "eww", "-p", ",".join(map(str, sorted(pids))), "-o", "pid=,command="],
            capture_output=True, text=True,
        )
        if result.returncode not in (0, 1):
            raise RuntimeError(f"Cannot check Gradle daemon shutdown: {result.stderr}")
        # Log PIDs may have been reused. Only wait for this build's marker.
        pids = {int(line.split()[0]) for line in result.stdout.splitlines()
                if marker in f" {line} "}
        if not pids:
            return
        if time.monotonic() >= deadline:
            raise RuntimeError(f"Gradle daemons have not exited; profiles are not ready: {sorted(pids)}")
        time.sleep(0.1)


def package_essentials(build_dir, dev_llvm, essentials_llvm, components, env):
    staging = build_dir.parent / "essentials-install"
    staging.mkdir()
    # Apply package.py's essentials settings only after the full dev install.
    run([
        "cmake", build_dir, *llvm_package.distribution_cmake_flags(components),
    ], env=env)
    # DESTDIR stages the configured install prefix without modifying the dev package.
    run(["ninja", "-C", build_dir, "install-distribution"], env={**env, "DESTDIR": str(staging)})
    staged_install = staging / dev_llvm.relative_to(dev_llvm.anchor)
    staged_install.rename(essentials_llvm)
    archive = llvm_package.create_archive(str(essentials_llvm), str(essentials_llvm))
    llvm_package.create_checksum_file(archive, f"{archive}.sha256")


def seed_gradle_home(source, destination):
    """Clone downloaded dependencies and Gradle distributions without sharing writes."""
    destination.mkdir()
    clonefile = ctypes.CDLL(None, use_errno=True).clonefile
    clonefile.argtypes = [ctypes.c_char_p, ctypes.c_char_p, ctypes.c_int]
    clonefile.restype = ctypes.c_int
    for relative in ("caches/modules-2", "wrapper/dists"):
        src, dst = source / relative, destination / relative
        if not src.is_dir():
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        print(f"Seed Gradle cache with copy-on-write: {src} -> {dst}", flush=True)
        if clonefile(os.fsencode(src), os.fsencode(dst), 0) != 0:
            error = ctypes.get_errno()
            raise OSError(error, f"Cannot clone {src} to {dst}: {os.strerror(error)}")
    # Gradle dependency-cache lock state and GC bookkeeping are not portable.
    modules = destination / "caches/modules-2"
    for name in ("modules-2.lock", "gc.properties"):
        (modules / name).unlink(missing_ok=True)


def find_xcode(version=None, build=None):
    """Find an Xcode in /Applications by version or by build number."""
    for app in sorted(Path("/Applications").glob("Xcode*.app")):
        version_plist = app / "Contents/version.plist"
        if not version_plist.is_file():
            continue
        info = plistlib.loads(version_plist.read_bytes())
        installed_version = info.get("CFBundleShortVersionString", "")
        installed_build = info.get("ProductBuildVersion", "")
        if (version is None or installed_version == version) and (build is None or installed_build == build):
            return app / "Contents/Developer", installed_version, installed_build
    requirement = f"build {build}" if build is not None else f"version {version}"
    raise RuntimeError(f"Xcode with {requirement} is not installed in /Applications")


def required_xcode_build():
    """Return the Xcode build required by Kotlin/Native (konan.properties: xcodeBuild)."""
    properties = KOTLIN_REPO / "kotlin-native/konan/konan.properties"
    for line in properties.read_text().splitlines():
        key, separator, value = line.partition("=")
        if separator and key.strip() == "xcodeBuild":
            return value.strip()
    raise RuntimeError(f"Cannot read xcodeBuild from {properties}")


def prepare_local_plugins(kotlin_repo, gradle_home):
    jars = {}
    for name, (_, pattern) in LOCAL_PLUGINS.items():
        matches = [p for p in kotlin_repo.glob(pattern)
                   if not p.name.endswith(("-sources.jar", "-javadoc.jar", "-shadow.jar", "-original.jar"))]
        if len(matches) != 1:
            raise RuntimeError(f"Expected one {name} JAR, got {matches}")
        jars[name] = str(matches[0].resolve())
    init_dir = gradle_home / "init.d"
    init_dir.mkdir(parents=True, exist_ok=True)
    replacements = "[\n" + ",\n".join(
        f"    {json.dumps(name)}: {json.dumps(jar)}" for name, jar in jars.items()
    ) + "\n]"
    init_dir.joinpath("kotlin-training-plugins.gradle").write_text(
        TRAINING_PLUGIN_INIT.replace("@PLUGIN_REPLACEMENTS@", replacements)
    )


def main():
    parser = build_parser()
    args = parser.parse_args()
    if args.version_number <= 0:
        parser.error("--version-number must be positive")
    if platform.system() != "Darwin" or platform.machine() != "arm64":
        parser.error("This workflow requires macOS arm64.")

    script_dir = Path(__file__).resolve().parent
    kotlin_repo = KOTLIN_REPO
    try:
        llvm_xcode, llvm_version, llvm_build = find_xcode(version=args.llvm_xcode_version)
    except RuntimeError as error:
        parser.error(str(error))
    print(f"LLVM Xcode: {llvm_version} ({llvm_build}) at {llvm_xcode}", flush=True)

    use_internal_server = os.environ.get("ORG_GRADLE_PROJECT_kotlin.native.useInternalServer")
    kotlin_xcode = None
    if (os.environ.get("KONAN_USE_INTERNAL_SERVER", "0") not in ("", "0") # auto mode is assumed to resolve to success
            or (use_internal_server is not None and use_internal_server.strip().lower() in ("", "true"))):
        print("Kotlin/Native Xcode: skipped (internal server is enabled)", flush=True)
    else:
        try:
            kotlin_xcode, kotlin_version, kotlin_build = find_xcode(build=required_xcode_build())
        except RuntimeError as error:
            parser.error(str(error))
        print(f"Kotlin/Native Xcode: {kotlin_version} ({kotlin_build}) at {kotlin_xcode}", flush=True)
    kotlin_xcode_env = {} if kotlin_xcode is None else {"DEVELOPER_DIR": str(kotlin_xcode)}
    package_script = script_dir / "package.py"
    stage0 = args.stage0.resolve(strict=True) if args.stage0 else None
    llvm_sources = args.llvm_sources.resolve(strict=True) if args.llvm_sources else None
    output = args.output_dir.resolve()
    # Refuse an existing output directory rather than mixing training profiles.
    output.mkdir()
    work_dir = Path(tempfile.mkdtemp(prefix=".work_dir.", dir=output))
    instrumented_llvm = work_dir / "llvm-instrumented"
    training_compiler = work_dir / "kn-instrumented"
    training_project = work_dir / "training-project"
    profile = output / "common.profdata"
    gradle_home = work_dir / "gradle-home"
    gradle_seed = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")).expanduser().resolve()
    # Each stage must exit its JVM and flush its profiles before the next stage.
    gradle_opts = os.environ.get("GRADLE_OPTS", "").strip()
    gradle_opts = f"{gradle_opts} -Dorg.gradle.daemon=false -Dorg.gradle.configuration-cache=false".strip()
    gradle_env = {
        "GRADLE_USER_HOME": str(gradle_home), "GRADLE_OPTS": gradle_opts,
        "KONAN_PGO_BUILD_PROCESS_MARKER": str(gradle_home), # only used for identifying the build process
    }

    try:
        seed_gradle_home(gradle_seed, gradle_home)
        for name in ["instrumented", "optimized", "build-raw", "training-raw"]:
            (work_dir / name).mkdir(exist_ok=True)

        print("1/5: Build instrumented LLVM", flush=True)
        package_args = ["--lto", "thin", "--save-temporary-files"]
        if llvm_sources:
            package_args += ["--llvm-sources", llvm_sources]
        else:
            llvm_sources = work_dir / "instrumented/llvm-project"
        if stage0:
            package_args += ["--stage0", stage0, "--num-stages", "1"]
        else:
            stage0 = work_dir / "instrumented/llvm-stage-1"
        run([
            sys.executable, package_script, *package_args,
            "--pgo-instrument", "--install-path", instrumented_llvm,
        ], cwd=work_dir / "instrumented", env={"DEVELOPER_DIR": str(llvm_xcode)})
        if not args.save_temporary_files:
            for build in (work_dir / "instrumented").glob("llvm-stage-*-build"):
                shutil.rmtree(build)

        print("2/5: Build instrumented Kotlin/Native for training", flush=True)
        native_tasks = [":kotlin-native:dist"]
        if args.kotlin_native_target:
            native_tasks += [
                f":kotlin-native:{args.kotlin_native_target}CrossDist",
                f":kotlin-native:{args.kotlin_native_target}PlatformLibs",
            ]
        else:
            native_tasks += [":kotlin-native:crossDist", ":kotlin-native:crossDistPlatformLibs"]
        run([
            "./gradlew",
            "-Pkotlin.native.enabled=true",
            f"-Pkotlin.native.llvm={instrumented_llvm}",
            "-Pkotlin.internal.native.llvm.pgoInstrument=true",
            *native_tasks,
            *[task for task, _ in LOCAL_PLUGINS.values()], # package compiler plugins
        ], cwd=kotlin_repo, env={
            **kotlin_xcode_env,
            **gradle_env,
            "LLVM_PROFILE_FILE": str(work_dir / "build-raw/%m-%p.profraw"),
        })
        wait_for_gradle_daemons(gradle_home)
        shutil.copytree(kotlin_repo / "kotlin-native/dist", training_compiler, symlinks=True)

        prepare_local_plugins(kotlin_repo, gradle_home)

        print("3/5: Compile training project", flush=True)
        run([
            "git", "clone", "--depth", "1", "--branch", args.training_branch,
            args.training_repo, training_project,
        ])
        run(["bash", "-euo", "pipefail", "-c", args.training_command], cwd=training_project, env={
            **kotlin_xcode_env,
            **gradle_env,
            "LLVM_PROFILE_FILE": str(work_dir / "training-raw/%m-%p.profraw"),
            "ORG_GRADLE_PROJECT_kotlin.native.home": str(training_compiler),
        })

        # Wait for daemon processes to exit and finish writing profiles before merging.
        wait_for_gradle_daemons(gradle_home)
        print("4/5: Merge training profiles", flush=True)
        raw_profiles = sorted((work_dir / "training-raw").glob("*.profraw"))
        if not raw_profiles:
            raise RuntimeError("The training command produced no LLVM profiles.")
        run([stage0 / "bin/llvm-profdata", "merge", *raw_profiles, "-o", profile])
        if not args.save_temporary_files:
            for name in [
                "llvm-instrumented", "kn-instrumented", "training-project",
                "build-raw", "training-raw", "gradle-home",
            ]:
                path = work_dir / name
                if path.exists():
                    shutil.rmtree(path)

        print("5/5: Build optimized LLVM and package dev and essentials distributions", flush=True)
        dev_name, essentials_name = artifact_names(args.version_number)
        dev_llvm = output / dev_name
        package_args = [
            "--llvm-sources", llvm_sources,
            "--stage0", stage0,
            "--num-stages", "1",
            "--lto", "thin",
            "--pgo-profile", profile,
            "--install-path", dev_llvm,
            # Both installs must finish before the outer workflow removes the build directory.
            "--save-temporary-files",
            "--pack",
        ]
        run([sys.executable, package_script, *package_args], cwd=work_dir / "optimized", env={"DEVELOPER_DIR": str(llvm_xcode)})
        package_essentials(
            work_dir / "optimized/llvm-stage-1-build", dev_llvm, output / essentials_name, args.distribution_components,
            {"DEVELOPER_DIR": str(llvm_xcode)},
        )
    # Failed runs retain unfinished intermediates for diagnostics. Cleanup below runs only on success.
    except BaseException:
        print(f"Unfinished intermediates retained at: {work_dir}", file=sys.stderr)
        raise
    else:
        if not args.save_temporary_files:
            shutil.rmtree(work_dir)

    print(f"PGO LLVM distribution and shared profile: {output}")


if __name__ == "__main__":
    main()
