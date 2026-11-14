#!/usr/bin/env python3
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license.

import argparse
import os
import platform
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


def build_parser():
    parser = argparse.ArgumentParser(
        description="Build and package LLVM with one shared IR PGO profile on macOS arm64.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument("--training-repo", required=True, help="Git remote of the training project")
    parser.add_argument("--training-branch", required=True, help="Branch or tag to clone")
    parser.add_argument("--training-command", required=True, help="Shell command to prepare and compile the training project")
    parser.add_argument("--install-path", type=Path, default=Path("llvm-pgo-distribution"), help="Output directory")
    parser.add_argument("--target", default="ios_simulator_arm64", help="Native target to build")
    parser.add_argument("--stage0", type=Path, help="Existing LLVM bootstrap; otherwise build one")
    parser.add_argument("--llvm-sources", type=Path, help="Existing LLVM checkout; otherwise use package.py's default")
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


def main():
    parser = build_parser()
    args = parser.parse_args()
    if platform.system() != "Darwin" or platform.machine() != "arm64":
        parser.error("This workflow requires macOS arm64.")

    script_dir = Path(__file__).resolve().parent
    kotlin_repo = script_dir.parents[2]
    package_script = script_dir / "package.py"
    stage0 = args.stage0.resolve(strict=True) if args.stage0 else None
    llvm_sources = args.llvm_sources.resolve(strict=True) if args.llvm_sources else None
    output = args.install_path.resolve()
    # Refuse an existing output directory rather than mixing training profiles.
    output.mkdir()
    work = Path(tempfile.mkdtemp(prefix=".work.", dir=output))
    instrumented_llvm = work / "llvm-instrumented"
    training_compiler = work / "kn-instrumented"
    training_project = work / "training-project"
    profile = output / "common.profdata"

    # Failed runs retain unfinished intermediates. Cleanup below runs only on success.
    try:
        for name in ["instrumented", "optimized", "build-raw", "training-raw"]:
            (work / name).mkdir()

        print("1/5: Build instrumented LLVM", flush=True)
        package_args = ["--lto", "thin", "--save-temporary-files"]
        if llvm_sources:
            package_args += ["--llvm-sources", llvm_sources]
        if stage0:
            package_args += ["--stage0", stage0, "--num-stages", "1"]
        run([
            sys.executable, package_script, *package_args,
            "--pgo-instrument", "--install-path", instrumented_llvm,
        ], cwd=work / "instrumented")
        if stage0 is None:
            stage0 = work / "instrumented/llvm-stage-1"
        if llvm_sources is None:
            llvm_sources = work / "instrumented/llvm-project"
        if not args.save_temporary_files:
            for build in (work / "instrumented").glob("llvm-stage-*-build"):
                shutil.rmtree(build)

        print("2/5: Build instrumented Kotlin/Native for training", flush=True)
        run([
            "./gradlew", "--no-daemon",
            "-Pkotlin.native.enabled=true",
            f"-Pkotlin.native.llvm={instrumented_llvm}",
            "-Pkotlin.internal.native.llvm.pgoInstrument=true",
            ":kotlin-native:dist",
            f":kotlin-native:{args.target}CrossDist",
            f":kotlin-native:{args.target}PlatformLibs",
        ], cwd=kotlin_repo, env={
            "GRADLE_USER_HOME": str(work / "gradle-kotlin"),
            "LLVM_PROFILE_FILE": str(work / "build-raw/%m-%p.profraw"),
        })
        shutil.copytree(kotlin_repo / "kotlin-native/dist", training_compiler, symlinks=True)

        print("3/5: Compile training project", flush=True)
        run([
            "git", "clone", "--depth", "1", "--branch", args.training_branch,
            args.training_repo, training_project,
        ])
        run(["bash", "-euo", "pipefail", "-c", args.training_command], cwd=training_project, env={
            "KOTLIN_NATIVE_HOME": str(training_compiler),
            "PATH": f"{training_compiler / 'bin'}:{os.environ['PATH']}",
            "GRADLE_USER_HOME": str(work / "gradle-training"),
            "GRADLE_OPTS": os.environ.get("GRADLE_OPTS", "") + " -Dorg.gradle.daemon=false",
            "LLVM_PROFILE_FILE": str(work / "training-raw/%m-%p.profraw"),
            "ORG_GRADLE_PROJECT_kotlin.native.home": str(training_compiler),
            "ORG_GRADLE_PROJECT_kotlin.native.toolchain.enabled": "false",
            "ORG_GRADLE_PROJECT_kotlin.native.disableCompilerDaemon": "true",
        })

        print("4/5: Merge training profiles", flush=True)
        raw_profiles = sorted((work / "training-raw").glob("*.profraw"))
        if not raw_profiles:
            raise RuntimeError("The training command produced no LLVM profiles.")
        run([stage0 / "bin/llvm-profdata", "merge", *raw_profiles, "-o", profile])
        if not args.save_temporary_files:
            for name in [
                "llvm-instrumented", "kn-instrumented", "training-project",
                "build-raw", "training-raw", "gradle-training",
            ]:
                path = work / name
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
        run([sys.executable, package_script, *package_args], cwd=work / "optimized")
    except BaseException:
        print(f"Unfinished intermediates retained at: {work}", file=sys.stderr)
        raise
    else:
        if not args.save_temporary_files:
            shutil.rmtree(work)

    print(f"PGO LLVM distribution and shared profile: {output}")


if __name__ == "__main__":
    main()
