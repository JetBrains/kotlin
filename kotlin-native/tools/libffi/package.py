#!/usr/bin/env python3
#
# Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license

"""
Downloads, patches, builds and packages libffi distributions for Kotlin/Native.

The script is driven by `manifests.py`, which declares the libffi version,
the distribution name and the platform-specific pre-build hooks (see the
`hooks/` directory) for every supported target (the same approach as
`llvm_builder`):

    python3 package.py                      # build for the current host
    python3 package.py --target mingw-x64   # cross-compile for Windows x64
    python3 package.py --list-targets       # show all targets from the manifest

The resulting distribution archive (`.tar.gz`, or `.zip` for Windows targets,
see `manifests.py`) and its SHA256 checksum are written to `--output-dir`
(the current directory by default).
"""

import argparse
import hashlib
import os
import platform
import shutil
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

import manifests


def run(command, cwd):
    print(f"+ {' '.join(map(str, command))}")
    subprocess.run(list(map(str, command)), cwd=cwd, check=True)


def download(url, destination: Path):
    print(f"Downloading {url} to {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(url, destination)


def detect_host_target():
    machine = platform.machine().lower()
    arch = {
        "x86_64": "x86-64",
        "amd64": "x86-64",
        "arm64": "arm64",
        "aarch64": "arm64",
    }.get(machine)
    if arch is None:
        sys.exit(f"Unsupported host machine: {machine}")
    if sys.platform == "darwin":
        return f"macos-{arch}"
    if sys.platform == "linux":
        return f"linux-{arch}"
    sys.exit(f"Unsupported host platform: {sys.platform}. Please pass --target explicitly.")


def fetch_sources(target, work_dir: Path) -> dict:
    version = target["version"]
    source = manifests.SOURCES[version]
    archive_name = f"libffi-{version}.tar.gz"
    download(source["url"], work_dir / archive_name)
    run(["tar", "-xf", archive_name], cwd=work_dir)
    if not (work_dir / source["directory"]).is_dir():
        sys.exit(f"Unexpected libffi archive layout: {source['directory']} does not exist")
    return source


def run_hook(hook, source_dir: Path):
    # Resolve against the script location: `__file__` may be relative (e.g. Python 3.8).
    hook_path = Path(__file__).resolve().parent / hook
    print(f"Running pre-build hook: {hook_path}")
    run(["sh", hook_path], cwd=source_dir)


def regenerate_configure(source_dir: Path):
    if (source_dir / "autogen.sh").exists():
        run(["./autogen.sh"], cwd=source_dir)
    else:
        run(["autoreconf", "-f", "-i"], cwd=source_dir)


def disable_docs(source_dir: Path) -> bool:
    makefile_am = source_dir / "Makefile.am"
    lines = makefile_am.read_text().splitlines(keepends=True)
    kept = [line for line in lines if not line.startswith("info_TEXINFOS")]
    if len(kept) == len(lines):
        return False
    makefile_am.write_text("".join(kept))
    return True


def build_and_install(target, source_dir: Path, distribution_dir: Path):
    configure_arguments = [
        "./configure",
        f"--prefix={distribution_dir}",
        "--disable-docs",
        "--enable-shared=no",
    ]
    configure_host = target.get("configure_host")
    if configure_host is not None:
        configure_arguments.insert(1, f"--host={configure_host}")
    run(configure_arguments, cwd=source_dir)
    run(["make", f"-j{os.cpu_count()}"], cwd=source_dir)
    run(["make", "install"], cwd=source_dir)


def normalize_install(target, distribution_dir: Path):
    # libffi up to 3.2.1 installs headers into `lib/libffi-<version>/include`
    # instead of a plain `include`; move them where Kotlin/Native expects them.
    version = target["version"]
    nested_include = distribution_dir / "lib" / f"libffi-{version}" / "include"
    if nested_include.is_dir() and not (distribution_dir / "include").is_dir():
        shutil.move(str(nested_include), distribution_dir / "include")
    shutil.rmtree(distribution_dir / "lib" / f"libffi-{version}", ignore_errors=True)
    shutil.rmtree(distribution_dir / "lib" / "pkgconfig", ignore_errors=True)
    shutil.rmtree(distribution_dir / "share", ignore_errors=True)


def sanity_check(distribution_dir: Path):
    static_library = distribution_dir / "lib" / "libffi.a"
    header = distribution_dir / "include" / "ffi.h"
    if not static_library.is_file() or not header.is_file():
        sys.exit(f"Sanity check failed: {static_library} or {header} is missing")


def create_archive(input_directory: Path, output_dir: Path, archive_type: str) -> Path:
    archive_base = output_dir / input_directory.name
    print(f"Creating archive {archive_base}.{archive_type} from {input_directory}")
    archive_format = {"tar.gz": "gztar", "zip": "zip"}.get(archive_type)
    if archive_format is None:
        sys.exit(f"Unsupported archive type: {archive_type}")
    shutil.make_archive(str(archive_base), archive_format, input_directory.parent, input_directory.name)
    return Path(str(archive_base) + f".{archive_type}")


def create_checksum_file(input_path: Path) -> Path:
    checksum = hashlib.sha256()
    with open(input_path, "rb") as input_contents:
        for chunk in iter(lambda: input_contents.read(4096), b""):
            checksum.update(chunk)
    checksum_path = Path(str(input_path) + ".sha256")
    checksum_path.write_text(checksum.hexdigest() + "\n")
    return checksum_path


def build_distribution(target, args):
    work_dir = Path(args.work_dir) if args.work_dir else Path(tempfile.mkdtemp(prefix="libffi-build-"))
    work_dir.mkdir(parents=True, exist_ok=True)
    try:
        source = fetch_sources(target, work_dir)
        source_dir = work_dir / source["directory"]
        disable_docs(source_dir)
        for hook in target["hooks"]:
            run_hook(hook, source_dir)
        # Sources fetched as a git tag archive have no pre-generated `configure`
        # script; hooks that edit `configure.ac` regenerate it themselves.
        if not (source_dir / "configure").is_file():
            regenerate_configure(source_dir)
        distribution_dir = work_dir / target["distribution"]
        build_and_install(target, source_dir, distribution_dir)
        normalize_install(target, distribution_dir)
        sanity_check(distribution_dir)
        if not args.no_pack:
            output_dir = Path(args.output_dir)
            output_dir.mkdir(parents=True, exist_ok=True)
            archive = create_archive(distribution_dir, output_dir, target.get("archive", "tar.gz"))
            checksum = create_checksum_file(archive)
            print(f"Created {archive} and {checksum}")
        else:
            print(f"libffi distribution is available at {distribution_dir}")
    finally:
        if not args.keep_work_dir and args.work_dir is None:
            print(f"Removing temporary directory: {work_dir}")
            shutil.rmtree(work_dir, ignore_errors=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Build libffi distribution for Kotlin/Native")
    parser.add_argument("--target", type=str, default=None,
                        help="Target to build for (see --list-targets); detected from the host by default")
    parser.add_argument("--list-targets", action="store_true",
                        help="List all targets from the manifest and exit")
    parser.add_argument("--output-dir", type=str, default=".",
                        help="Where the distribution archive and its checksum will be placed")
    parser.add_argument("--work-dir", type=str, default=None,
                        help="Directory to build in (a temporary directory by default)")
    parser.add_argument("--keep-work-dir", action="store_true",
                        help="Do not remove the temporary working directory after the build")
    parser.add_argument("--no-pack", action="store_true",
                        help="Do not create the distribution archive and checksum")
    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()
    if args.list_targets:
        for target_name, target in manifests.TARGETS.items():
            print(f"{target_name}: libffi {target['version']} -> {target['distribution']} ({target.get('archive', 'tar.gz')})")
        return
    target_name = args.target if args.target is not None else detect_host_target()
    target = manifests.TARGETS.get(target_name)
    if target is None:
        available = ", ".join(manifests.TARGETS)
        sys.exit(f"Unknown target: {target_name}. Available targets: {available}")
    print(f"Building libffi {target['version']} for {target_name} as {target['distribution']}")
    build_distribution(target, args)


if __name__ == "__main__":
    main()
