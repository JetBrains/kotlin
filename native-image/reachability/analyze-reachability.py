#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Summarize GraalVM reachability metadata hotspots."
    )
    parser.add_argument(
        "json_file",
        nargs="?",
        default=Path(__file__).with_name("reachability-metadata.json"),
        type=Path,
        help="Path to reachability metadata JSON.",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=15,
        help="Rows to show in top sections.",
    )
    parser.add_argument(
        "--tail",
        type=int,
        default=15,
        help="Rows to show in tail sections.",
    )
    return parser.parse_args()


def normalize_type_name(type_value: Any) -> str:
    if isinstance(type_value, str):
        return type_value
    if isinstance(type_value, dict):
        lambda_info = type_value.get("lambda")
        if isinstance(lambda_info, dict):
            declaring_class = lambda_info.get("declaringClass", "<unknown>")
            interfaces = ",".join(lambda_info.get("interfaces", []))
            if interfaces:
                return f"lambda@{declaring_class} -> {interfaces}"
            return f"lambda@{declaring_class}"
    return json.dumps(type_value, sort_keys=True)


def strip_array_suffix(type_name: str) -> str:
    while type_name.endswith("[]"):
        type_name = type_name[:-2]
    return type_name


def package_name(type_name: str) -> str:
    base_name = strip_array_suffix(type_name)
    if "lambda@" in base_name:
        base_name = base_name.split(" -> ", 1)[0].split("@", 1)[1]
    if "." not in base_name:
        return "<default>"
    return base_name.rsplit(".", 1)[0]


def family_name(package: str) -> str:
    if package == "<default>":
        return package

    parts = package.split(".")
    if parts[0] in {"org", "com"} and len(parts) >= 3:
        return ".".join(parts[:3])
    if parts[0] in {"java", "javax", "sun", "jdk", "kotlin", "kotlinx"} and len(parts) >= 2:
        return ".".join(parts[:2])
    if len(parts) >= 2:
        return ".".join(parts[:2])
    return package


def resource_family(glob: str) -> str:
    if glob.startswith("META-INF/services/"):
        return "META-INF/services"
    if "/" in glob:
        return glob.split("/", 1)[0]
    return "<file>"


def blank_metrics() -> dict[str, Any]:
    return {
        "entries": 0,
        "methods": 0,
        "fields": 0,
        "jni": 0,
        "samples": [],
    }


def add_sample(bucket: dict[str, Any], sample: str) -> None:
    if sample not in bucket["samples"] and len(bucket["samples"]) < 3:
        bucket["samples"].append(sample)


def score(metrics: dict[str, Any]) -> int:
    return metrics["entries"] + metrics["methods"] + metrics["fields"] + metrics["jni"]


def print_table(title: str, rows: list[dict[str, Any]], tail: bool = False) -> None:
    print(title)
    if not rows:
        print("  <none>")
        print()
        return

    header = f"{'score':>5} {'count':>5} {'meth':>5} {'fld':>5} {'jni':>4}  name"
    print(header)
    print(f"{'-' * 5} {'-' * 5} {'-' * 5} {'-' * 5} {'-' * 4}  {'-' * 24}")
    for row in rows:
        label = row["name"]
        if row.get("samples"):
            label = f"{label} | {', '.join(row['samples'])}"
        print(
            f"{row['score']:>5} {row['entries']:>5} {row['methods']:>5} "
            f"{row['fields']:>5} {row['jni']:>4}  {label}"
        )
    print()


def summarize_reflection(data: dict[str, Any], top: int, tail: int) -> None:
    reflection = data.get("reflection", [])
    package_buckets: dict[str, dict[str, Any]] = defaultdict(blank_metrics)
    family_buckets: dict[str, dict[str, Any]] = defaultdict(blank_metrics)
    exact_rows: list[dict[str, Any]] = []
    array_count = 0

    total_methods = 0
    total_fields = 0
    total_jni = 0

    for item in reflection:
        type_name = normalize_type_name(item.get("type"))
        pkg = package_name(type_name)
        fam = family_name(pkg)
        methods = len(item.get("methods", []))
        fields = len(item.get("fields", []))
        jni = 1 if item.get("jniAccessible") else 0

        if "[]" in type_name:
            array_count += 1

        total_methods += methods
        total_fields += fields
        total_jni += jni

        exact_rows.append(
            {
                "name": type_name,
                "entries": 1,
                "methods": methods,
                "fields": fields,
                "jni": jni,
                "score": 1 + methods + fields + jni,
            }
        )

        for bucket_name, bucket_map in ((pkg, package_buckets), (fam, family_buckets)):
            bucket = bucket_map[bucket_name]
            bucket["entries"] += 1
            bucket["methods"] += methods
            bucket["fields"] += fields
            bucket["jni"] += jni
            add_sample(bucket, type_name)

    print("== Reflection summary ==")
    print(f"entries: {len(reflection)}")
    print(f"distinct packages: {len(package_buckets)}")
    print(f"distinct families: {len(family_buckets)}")
    print(f"method directives: {total_methods}")
    print(f"field directives: {total_fields}")
    print(f"jni-accessible entries: {total_jni}")
    print(f"array-like type entries: {array_count}")
    print()

    exact_rows.sort(key=lambda row: (-row["score"], -row["fields"], -row["methods"], row["name"]))
    print_table("Top exact reflection offenders", exact_rows[:top])

    package_rows = [
        {
            "name": name,
            "entries": metrics["entries"],
            "methods": metrics["methods"],
            "fields": metrics["fields"],
            "jni": metrics["jni"],
            "score": score(metrics),
            "samples": metrics["samples"],
        }
        for name, metrics in package_buckets.items()
    ]
    package_rows.sort(
        key=lambda row: (-row["score"], -row["entries"], -row["fields"], -row["methods"], row["name"])
    )
    print_table("Top packages by reachability burden", package_rows[:top])

    family_rows = [
        {
            "name": name,
            "entries": metrics["entries"],
            "methods": metrics["methods"],
            "fields": metrics["fields"],
            "jni": metrics["jni"],
            "score": score(metrics),
            "samples": metrics["samples"],
        }
        for name, metrics in family_buckets.items()
    ]
    family_rows.sort(
        key=lambda row: (-row["score"], -row["entries"], -row["fields"], -row["methods"], row["name"])
    )
    print_table("Top families by reachability burden", family_rows[:top])

    tail_rows = sorted(
        package_rows,
        key=lambda row: (row["score"], row["entries"], row["fields"], row["methods"], row["name"]),
    )
    print_table("Reflection long tail", tail_rows[:tail], tail=True)


def summarize_resources(data: dict[str, Any], top: int, tail: int) -> None:
    resources = data.get("resources", [])
    module_counts: Counter[str] = Counter()
    family_counts: Counter[str] = Counter()
    service_package_counts: Counter[str] = Counter()
    resource_rows: list[dict[str, Any]] = []

    for item in resources:
        glob = item.get("glob", "<missing glob>")
        module_name = item.get("module", "<none>")
        module_counts[module_name] += 1
        family_counts[resource_family(glob)] += 1

        service_prefix = "META-INF/services/"
        if glob.startswith(service_prefix):
            iface = glob[len(service_prefix) :]
            pkg = iface.rsplit(".", 1)[0] if "." in iface else "<default>"
            service_package_counts[pkg] += 1

        resource_rows.append(
            {
                "name": glob,
                "entries": 1,
                "methods": 0,
                "fields": 0,
                "jni": 0,
                "score": 1,
            }
        )

    print("== Resource summary ==")
    print(f"entries: {len(resources)}")
    print(
        "explicit module fields: "
        f"{sum(1 for item in resources if 'module' in item)}"
    )
    print()

    module_rows = [
        {
            "name": name,
            "entries": count,
            "methods": 0,
            "fields": 0,
            "jni": 0,
            "score": count,
        }
        for name, count in module_counts.items()
    ]
    module_rows.sort(key=lambda row: (-row["score"], row["name"]))
    print_table("Resource modules (explicit JSON field only)", module_rows[:top])

    family_rows = [
        {
            "name": name,
            "entries": count,
            "methods": 0,
            "fields": 0,
            "jni": 0,
            "score": count,
        }
        for name, count in family_counts.items()
    ]
    family_rows.sort(key=lambda row: (-row["score"], row["name"]))
    print_table("Resource families", family_rows[:top])

    service_rows = [
        {
            "name": name,
            "entries": count,
            "methods": 0,
            "fields": 0,
            "jni": 0,
            "score": count,
        }
        for name, count in service_package_counts.items()
    ]
    service_rows.sort(key=lambda row: (-row["score"], row["name"]))
    print_table("Service descriptor packages", service_rows[:top])

    resource_rows.sort(key=lambda row: row["name"])
    print_table("Resource long tail", resource_rows[:tail], tail=True)


def main() -> int:
    args = parse_args()
    data = json.loads(args.json_file.read_text())

    print(f"Reachability analysis: {args.json_file}")
    print()
    summarize_reflection(data, args.top, args.tail)
    summarize_resources(data, args.top, args.tail)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
