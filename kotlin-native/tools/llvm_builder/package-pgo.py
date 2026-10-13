#!/usr/bin/env python3
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license.

import argparse
import json
import os
import platform
import plistlib
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


# newer xcode's libc++ versions reject parts of LLVM 21.1.6's sources.
DEFAULT_LLVM_XCODE_VERSION = "26.4"
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
    parser.add_argument("--install-path", type=Path, default=Path("llvm-pgo-distribution"), help="Output directory")
    parser.add_argument("--target", help="Kotlin/Native target to build platform libraries for; by default all targets are built")
    parser.add_argument("--stage0", type=Path, help="Existing LLVM bootstrap; otherwise build one")
    parser.add_argument("--llvm-sources", type=Path, help="Existing LLVM checkout; otherwise use package.py's default")
    parser.add_argument(
        "--xcode-llvm-version", default=DEFAULT_LLVM_XCODE_VERSION,
        help="Xcode version used for the LLVM builds; its macOS SDK must be compatible with the LLVM sources",
    )
    parser.add_argument("--build-targets", nargs="+", default=["install-distribution"], help="Final LLVM install targets")
    parser.add_argument(
        "--distribution-components", nargs="+",
        default=["clang", "libclang", "lld", "llvm-cov", "llvm-profdata", "llvm-ar", "clang-resource-headers"],
        help="Final LLVM distribution components",
    )
    parser.add_argument("--save-temporary-files", action="store_true", help="Keep intermediate builds, training project and profiles")
    return parser


def run(command, cwd=None, env=None):
    subprocess.run(command, cwd=cwd, env={**os.environ, **(env or {})}, check=True)


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
    init_dir = gradle_home.parent / "gradle-training" / "init.d"
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
    if platform.system() != "Darwin" or platform.machine() != "arm64":
        parser.error("This workflow requires macOS arm64.")

    script_dir = Path(__file__).resolve().parent
    kotlin_repo = KOTLIN_REPO
    try:
        llvm_xcode, llvm_version, llvm_build = find_xcode(version=args.xcode_llvm_version)
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
    output = args.install_path.resolve()
    # Refuse an existing output directory rather than mixing training profiles.
    output.mkdir()
    work_dir = Path(tempfile.mkdtemp(prefix=".work_dir.", dir=output))
    instrumented_llvm = work_dir / "llvm-instrumented"
    training_compiler = work_dir / "kn-instrumented"
    training_project = work_dir / "training-project"
    profile = output / "common.profdata"

    try:
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
        if args.target:
            native_tasks += [
                f":kotlin-native:{args.target}CrossDist",
                f":kotlin-native:{args.target}PlatformLibs",
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
            "GRADLE_USER_HOME": str(work_dir / "gradle-kotlin"),
            "LLVM_PROFILE_FILE": str(work_dir / "build-raw/%m-%p.profraw"),
        })
        shutil.copytree(kotlin_repo / "kotlin-native/dist", training_compiler, symlinks=True)

        prepare_local_plugins(kotlin_repo, work_dir / "gradle-kotlin")

        print("3/5: Compile training project", flush=True)
        run([
            "git", "clone", "--depth", "1", "--branch", args.training_branch,
            args.training_repo, training_project,
        ])
        # TODO: verify the clean-host repository/proxy setup before making any
        # Gradle network workaround part of this workflow.
        gradle_opts = os.environ.get("GRADLE_OPTS", "").strip()
        gradle_opts = f"{gradle_opts} -Dorg.gradle.configuration-cache=false".strip()
        run(["bash", "-euo", "pipefail", "-c", args.training_command], cwd=training_project, env={
            **kotlin_xcode_env,
            "GRADLE_USER_HOME": str(work_dir / "gradle-training"),
            "GRADLE_OPTS": gradle_opts,
            "LLVM_PROFILE_FILE": str(work_dir / "training-raw/%m-%p.profraw"),
            "ORG_GRADLE_PROJECT_kotlin.native.home": str(training_compiler),
        })

        print("4/5: Merge training profiles", flush=True)
        raw_profiles = sorted((work_dir / "training-raw").glob("*.profraw"))
        if not raw_profiles:
            raise RuntimeError("The training command produced no LLVM profiles.")
        run([stage0 / "bin/llvm-profdata", "merge", *raw_profiles, "-o", profile])
        if not args.save_temporary_files:
            for name in [
                "llvm-instrumented", "kn-instrumented", "training-project",
                "build-raw", "training-raw", "gradle-training",
            ]:
                path = work_dir / name
                if path.exists():
                    shutil.rmtree(path)

        print("5/5: Build and package optimized LLVM", flush=True)
        package_args = [
            "--llvm-sources", llvm_sources,
            "--stage0", stage0,
            "--num-stages", "1",
            "--lto", "thin",
            "--pgo-profile", profile,
            "--install-path", output / "llvm",
            "--build-targets", *args.build_targets,
            "--distribution-components", *args.distribution_components,
            "--pack",
        ]
        if args.save_temporary_files:
            package_args.append("--save-temporary-files")
        run([sys.executable, package_script, *package_args], cwd=work_dir / "optimized", env={"DEVELOPER_DIR": str(llvm_xcode)})
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
