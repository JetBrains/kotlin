from subprocess import DEVNULL, PIPE
from shutil import copyfile
from pathlib import Path
from typing import List
import urllib.request
import subprocess
import argparse
import platform
import tempfile
import tarfile
import sys
import os


def main():
    argparser = argparse.ArgumentParser(description="Finds symbols from 'Required reason API' list")
    argparser.add_argument("-t", "--target", type=str, default="iosArm64", help='Kotlin target to inspect')
    argparser.add_argument("-v", "--verbose", help="enable verbose output", action="store_true")

    args = argparser.parse_args()
    verbose = args.verbose
    target = args.target

    konan_data_dir_env = os.getenv("KONAN_DATA_DIR")
    if konan_data_dir_env is not None:
        konan_data_dir = Path(konan_data_dir_env)
    else:
        konan_data_dir = Path.home() / ".konan"
    konan_data_dir.mkdir(exist_ok=True)

    if platform.machine() == "arm64":
        arch = "aarch64"
    else:
        arch = "x86_64"

    # TODO make it possible to use existing RC1/RC3/2.0.0 versions
    kotlin_version = "2.0.0-RC2"
    konan_home = konan_data_dir / f"kotlin-native-prebuilt-macos-{arch}-{kotlin_version}"

    if not konan_home.exists():
        download_dist(konan_home, kotlin_version, arch)

    libraries = filter(library_filter, retrieve_libraries_from_gradle_project(target, verbose))
    libraries = list(map(lambda it: it.strip(), libraries))

    all_clear = True
    all_clear = check_for_compose(libraries) and all_clear

    for library in libraries:
        all_clear = check_library(konan_home, library, verbose) and all_clear

    if all_clear:
        print("\n" + green("No usages of required reason APIs found"))


def download_dist(konan_home: Path, kotlin_version: str, arch: str):
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)
        tar = f"kotlin-native-prebuilt-{kotlin_version}-macos-{arch}"
        tar_path = tmp_path.with_suffix(".tar.gz")
        url = f"https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-native-prebuilt/{kotlin_version}/{tar}.tar.gz"

        print(f"Downloading {url}... ", end="")
        urllib.request.urlretrieve(url, tar_path)
        print(f"success!")

        print(f"Extracting into {konan_home}... ", end="")
        with tarfile.open(tar_path, "r:gz") as tar_file:
            tar_file.extractall(path=tmp_path)
        os.rename(tmp_path / os.path.basename(konan_home), konan_home)
        print(f"success!")


def find_gradlew(directory: Path):
    gradlew_file = directory / 'gradlew'
    if gradlew_file.exists():
        return gradlew_file
    else:
        parent = directory.parent
        if parent == directory:
            raise FileNotFoundError(f"Can't find gradlew in parent directories of {Path.cwd()}")
        return find_gradlew(parent)


def capitalize_first_letter(s: str):
    return s[0].upper() + s[1:]


def run_gradle_with_injected_code(injected_kts: str, injected: str, error: Exception, args: list[str], verbose: bool):
    working_dir = Path.cwd()
    gradlew_path = find_gradlew(working_dir)

    build_gradle_kts = working_dir / "build.gradle.kts"
    build_gradle = working_dir / "build.gradle"

    if build_gradle_kts.exists():
        build_gradle = build_gradle_kts
        injected = injected_kts
    elif not build_gradle.exists():
        raise FileNotFoundError(f"build.gradle.kts or build.gradle not found in {working_dir}")

    build_gradle_backup = build_gradle.with_suffix(".backup")
    copyfile(build_gradle, build_gradle_backup)
    try:
        with open(build_gradle, 'a+') as file:
            file.write(injected)
        result = subprocess.run(
            args=[str(gradlew_path)] + args,
            cwd=working_dir,
            stdout=None if verbose else DEVNULL,
            stderr=None if verbose else PIPE,
        )
        if result.returncode != 0:
            if result.stderr is not None:
                sys.stderr.buffer.write(result.stderr)
                sys.stderr.flush()
            raise error
    finally:
        os.replace(build_gradle_backup, build_gradle)


def retrieve_libraries_from_gradle_project(target: str, verbose: bool) -> list[str]:
    target = capitalize_first_letter(target)

    build_gradle_kts_inject = """
    run {
        val compileTask = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>("compileKotlin$target")
        tasks.register(compileTask.name + "Libraries") {
            val libraries = compileTask.map { it.libraries + it.outputFile.get() }
            val output = file("$name.txt")
            val mimallocWarning = org.jetbrains.kotlin.tooling.core.KotlinToolingVersion(org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion(logger)) < org.jetbrains.kotlin.tooling.core.KotlinToolingVersion("1.9.20")
            dependsOn(compileTask)
            doFirst {
                if (mimallocWarning) output.appendText("{{kotlin_mimalloc_warning}}\\n")
                libraries.get().forEach { output.appendText(it.absolutePath + "\\n") }
            }
        }
    }
    """.replace("$target", target)

    build_gradle_inject = """
    def _compileTask = tasks.named("compileKotlin$target")
    tasks.register(_compileTask.name + "Libraries") {
        def libraries = _compileTask.map { it.libraries + it.outputFile.get() }
        def output = file("${name}.txt")
        def mimallocWarning = org.jetbrains.kotlin.tooling.core.KotlinToolingVersionKt.KotlinToolingVersion(org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt.getKotlinPluginVersion(logger)) < org.jetbrains.kotlin.tooling.core.KotlinToolingVersionKt.KotlinToolingVersion("1.9.20")
        dependsOn(_compileTask)
        doFirst {
            if (mimallocWarning) output.append("{{kotlin_mimalloc_warning}}\\n")
            libraries.get().forEach { output.append(it.absolutePath + "\\n") }
        }
    }
    """.replace("$target", target)

    libraries_task = f"compileKotlin{target}Libraries"
    libraries_txt = Path.cwd() / (libraries_task + ".txt")

    error = ChildProcessError(f"Gradle invocation failed. Please check that the project can be compiled with '$PROJECT_ROOT/gradlew compileKotlin{target}'")

    try:
        print("Running Gradle to retrieve the list of dependencies... ", end="\n" if verbose else "")
        run_gradle_with_injected_code(build_gradle_kts_inject, build_gradle_inject, error, [libraries_task], verbose)
        if not verbose:
            print("success!")

        with open(libraries_txt, 'r') as file:
            return file.readlines()
    finally:
        if libraries_txt.exists():
            libraries_txt.unlink()


def library_filter(library: str):
    special = library.startswith("{{")
    name = os.path.basename(library).strip()
    platform_lib = name.startswith("org.jetbrains.kotlin.native.platform.")
    cinterop = ("-cinterop-" in name)
    klib = name.endswith(".klib")
    return special or (klib and not platform_lib and not cinterop)


def dump_imported_platform_signatures(konan_home: Path, library: str, verbose: bool):
    process = subprocess.run(
        args=[
            str(konan_home / "bin/klib"),
            "dump-ir-signatures",
            library,
        ],
        stdout=PIPE,
        stderr=None if verbose else DEVNULL,
    )

    if process.returncode != 0:
        print(f"warning: Failed to dump signatures from {library}, skipping")
        return list()

    lines = process.stdout.decode().splitlines()
    return list(filter(lambda line: line.startswith("platform."), lines))


def check_library(konan_home: Path, library: str, verbose: bool) -> bool:
    if library == "{{kotlin_mimalloc_warning}}":
        return print_kotlin_mimalloc_warning()

    required_reason_symbols = [
        # File timestamp APIs
        "platform.Foundation/NSFileCreationDate",
        "platform.Foundation/NSFileModificationDate",
        "platform.UIKit/UIDocument.fileModificationDate",
        "platform.Foundation/fileModificationDate",
        "platform.Foundation/NSURLContentModificationDateKey",
        "platform.Foundation/NSURLCreationDateKey",
        "platform.posix/getattrlist",
        "platform.posix/getattrlistbulk",
        "platform.posix/fgetattrlist",
        "platform.posix/stat",
        "platform.posix/fstat",
        "platform.posix/fstatat",
        "platform.posix/lstat",
        "platform.posix/getattrlistat",

        # System boot time APIs
        "platform.Foundation/NSProcessInfo.systemUptime",
        "platform.darwin/mach_absolute_time",

        # Disk space APIs
        "platform.Foundation/NSURLVolumeAvailableCapacityKey",
        "platform.Foundation/NSURLVolumeAvailableCapacityForImportantUsageKey",
        "platform.Foundation/NSURLVolumeAvailableCapacityForOpportunisticUsageKey",
        "platform.Foundation/NSURLVolumeTotalCapacityKey",
        "platform.Foundation/NSFileSystemFreeSize",
        "platform.Foundation/NSFileSystemSize",
        "platform.posix/getattrlist",
        "platform.posix/fgetattrlist",
        "platform.posix/getattrlistat",

        # Active keyboard APIs
        "platform.UIKit/UITextInputMode.Companion.activeInputModes",

        # User defaults APIs
        "platform.Foundation/NSUserDefaults",
    ]

    used_symbols = []

    library_name = os.path.basename(library)
    signatures = dump_imported_platform_signatures(konan_home, library, verbose)
    for symbol in required_reason_symbols:
        for signature in signatures:
            if signature.startswith(symbol):
                used_symbols.append(symbol)
                break

    if used_symbols:
        print(f"\nFound usages of required reason API in {yellow(library_name)} from {library}")
        print("\t" + "\n\t".join(used_symbols))
        return False
    elif verbose:
        print(f"\nNo usages of required reason API in {green(library_name)} from {library}")

    return True


def print_kotlin_mimalloc_warning():
    print(f"""
Kotlin is using {yellow("mach_absolute_time")} from the required reason API list in versions lower than 1.9.20
\tSee more details here: https://kotl.in/kkrs8t""")
    return False


def check_for_compose(libraries: List[str]) -> bool:
    for library in libraries:
        if os.path.basename(library) == "skiko.klib":
            print(f"""
Compose Multiplatform for iOS is using {yellow("stat")} and {yellow("fstat")} from from the required reason API list
\tSee more details here: https://kotl.in/rx45vh""")
            return False
    return True


def yellow(value: str) -> str:
    return Colors.yellow + value + Colors.reset


def green(value: str) -> str:
    return Colors.green + value + Colors.reset


class Colors:
    reset = "\u001B[0m"
    green = "\u001B[32m"
    yellow = "\u001B[33m"


main()
