#!/usr/bin/env python3
"""Summarize MEMBER_EXTENSION_TWO_PHASE_INFERENCE diagnostics from YAML compilation results."""

from __future__ import annotations

import argparse
import csv
import json
import random
import sys
from collections import Counter
from pathlib import Path
from typing import Literal, TypedDict, cast

import yaml

CALL_DIAGNOSTIC_NAME = "MEMBER_EXTENSION_TWO_PHASE_INFERENCE"
SUMMARY_DIAGNOSTIC_NAME = "MEMBER_EXTENSION_TWO_PHASE_INFERENCE_SUMMARY"
ANALYSIS_DIRECTORY = Path(__file__).parent


class ReceiverComparison(TypedDict):
    actualType: str | None
    inferredType: str | None
    relation: str


class NormalInference(TypedDict):
    inferredTypes: dict[str, str]
    receiver: ReceiverComparison
    receiverTypeParameters: dict[str, ReceiverComparison]
    receiverTypeConstructorApproximated: bool
    receiverParameters: dict[str, str]


class TwoPhaseSuccess(TypedDict):
    result: Literal["success"]
    outcome: Literal["successful", "inapplicable", "failed"]
    inferredTypes: dict[str, str]
    receiverPhaseFixed: list[str]
    receiverPhaseUnfixed: list[str]
    argumentPhaseFixed: list[str]


class TwoPhaseError(TypedDict):
    result: Literal["error"]
    reason: str


class DiagnosticMetadata(TypedDict):
    _inputPath: str
    _location: str


class CallDiagnostic(DiagnosticMetadata):
    callableId: str
    signature: str
    normalInference: NormalInference
    twoPhaseInference: TwoPhaseSuccess | TwoPhaseError


class CallableSummary(TypedDict):
    totalCalls: int
    successfulCalls: int
    inapplicableCalls: int
    failedCalls: int
    errorCalls: int
    receiverTypeConstructorApproximatedCalls: int
    receiverTypeParameterApproximatedCalls: int
    receiverTypeConstructorAndParameterApproximatedCalls: int
    receiverRelations: dict[str, int]


class SummaryDiagnostic(DiagnosticMetadata):
    callableId: str
    signature: str
    summary: CallableSummary


class AnalysisSummary(TypedDict):
    diagnosticCount: int
    callDiagnosticCount: int
    summaryDiagnosticCount: int
    analyzedCallCount: int
    malformedCount: int
    resultKinds: dict[str, int]
    outcomes: dict[str, int]
    receiverRelations: dict[str, int]
    receiverApproximations: dict[str, int]
    errorReasons: dict[str, int]
    topCallables: dict[str, int]
    topSignatures: dict[str, int]
    malformed: list[str]


class CallSite(TypedDict):
    inputPath: str
    location: str
    diagnostic: CallDiagnostic


class Arguments(argparse.Namespace):
    inputs: list[Path]
    json: bool
    inapplicable_csv: bool
    signature_summary_csv: bool
    sample_over_approximated_successful: int
    sample_inapplicable: int
    sample_failed: int
    top_inapplicable_callables: int
    top_over_approximated_callables: int
    inapplicable_details: str | None


def resolve_input_path(path: Path) -> Path:
    if path.is_absolute() or path.exists() or path.parent != Path("."):
        return path
    return ANALYSIS_DIRECTORY / path


def load_diagnostics(
    input_paths: list[Path],
) -> tuple[list[CallDiagnostic], list[SummaryDiagnostic], list[str]]:
    diagnostics: list[CallDiagnostic] = []
    summaries: list[SummaryDiagnostic] = []
    malformed: list[str] = []
    decoder = json.JSONDecoder()

    for input_path in input_paths:
        try:
            with input_path.open() as input_file:
                document: object = yaml.load(input_file, Loader=yaml.CSafeLoader)
        except yaml.YAMLError as error:
            malformed.append(f"{input_path}: invalid YAML: {error}")
            continue
        if not isinstance(document, dict):
            malformed.append(f"{input_path}: YAML document is not a mapping")
            continue
        document = cast(dict[str, object], document)
        entries_by_compilation = document.get("compilation-diagnostics-log")
        if not isinstance(entries_by_compilation, dict):
            malformed.append(f"{input_path}: compilation-diagnostics-log is not a mapping")
            continue

        for compilation_name, entries in entries_by_compilation.items():
            compilation_location = f"{input_path}: {compilation_name}"
            if not isinstance(compilation_name, str) or not isinstance(entries, list):
                malformed.append(f"{compilation_location}: compilation name or diagnostics list has an invalid type")
                continue

            for index, entry in enumerate(entries, start=1):
                entry_location = f"{compilation_location}: diagnostic {index}"
                if not isinstance(entry, dict):
                    malformed.append(f"{entry_location}: diagnostic is not a mapping")
                    continue
                entry = cast(dict[str, object], entry)
                name = entry.get("name")
                if name not in (CALL_DIAGNOSTIC_NAME, SUMMARY_DIAGNOSTIC_NAME):
                    continue
                message = entry.get("message")
                location = entry.get("location")
                if not isinstance(message, str) or not isinstance(location, str):
                    malformed.append(f"{entry_location}: location or message is not a string")
                    continue

                marker = f"{name}:"
                marker_start = message.find(marker)
                if marker_start < 0:
                    malformed.append(f"{entry_location}: diagnostic marker not found in message")
                    continue
                object_start = message.find("{", marker_start + len(marker))
                if object_start < 0:
                    malformed.append(f"{entry_location}: JSON object not found")
                    continue
                try:
                    value, _ = decoder.raw_decode(message[object_start:])
                except json.JSONDecodeError as error:
                    malformed.append(f"{entry_location}: {error.msg}")
                    continue
                if isinstance(value, dict):
                    value = cast(dict[str, object], value)
                    value["_inputPath"] = compilation_location
                    value["_location"] = location
                    if name == SUMMARY_DIAGNOSTIC_NAME:
                        summaries.append(cast(SummaryDiagnostic, value))
                    else:
                        diagnostics.append(cast(CallDiagnostic, value))
                else:
                    malformed.append(f"{entry_location}: diagnostic payload is not an object")

    return diagnostics, summaries, malformed


def summarize(
    diagnostics: list[CallDiagnostic],
    summaries: list[SummaryDiagnostic],
    malformed: list[str],
) -> AnalysisSummary:
    result_kinds: Counter[str] = Counter()
    outcomes: Counter[str] = Counter()
    receiver_relations: Counter[str] = Counter()
    receiver_approximations: Counter[str] = Counter()
    callables: Counter[str] = Counter()
    signatures: Counter[tuple[str, str]] = Counter()
    error_reasons: Counter[str] = Counter()

    for summary_diagnostic in summaries:
        summary = summary_diagnostic["summary"]
        successful = summary["successfulCalls"]
        inapplicable = summary["inapplicableCalls"]
        failed = summary["failedCalls"]
        errors = summary["errorCalls"]
        callables[summary_diagnostic["callableId"]] += summary["totalCalls"]
        signatures[summary_diagnostic["callableId"], summary_diagnostic["signature"]] += summary["totalCalls"]
        result_kinds["success"] += successful + inapplicable + failed
        result_kinds["error"] += errors
        outcomes["successful"] += successful
        outcomes["inapplicable"] += inapplicable
        outcomes["failed"] += failed
        receiver_relations.update(summary["receiverRelations"])
        receiver_approximations["receiverTypeConstructorApproximatedCalls"] += summary[
            "receiverTypeConstructorApproximatedCalls"
        ]
        receiver_approximations["receiverTypeParameterApproximatedCalls"] += summary[
            "receiverTypeParameterApproximatedCalls"
        ]
        receiver_approximations["receiverTypeConstructorAndParameterApproximatedCalls"] += summary[
            "receiverTypeConstructorAndParameterApproximatedCalls"
        ]

    for call_diagnostic in diagnostics:
        result = call_diagnostic["twoPhaseInference"]
        if result["result"] == "error":
            error_reasons[result["reason"]] += 1

    return {
        "diagnosticCount": len(diagnostics) + len(summaries),
        "callDiagnosticCount": len(diagnostics),
        "summaryDiagnosticCount": len(summaries),
        "analyzedCallCount": sum(callables.values()),
        "malformedCount": len(malformed),
        "resultKinds": dict(result_kinds.most_common()),
        "outcomes": dict(outcomes.most_common()),
        "receiverRelations": dict(receiver_relations.most_common()),
        "receiverApproximations": dict(receiver_approximations.items()),
        "errorReasons": dict(error_reasons.most_common()),
        "topCallables": dict(callables.most_common(25)),
        "topSignatures": {
            f"{callable_id} {signature}": count
            for (callable_id, signature), count in signatures.most_common(25)
        },
        "malformed": malformed,
    }


def sample_analyzed_calls(
    diagnostics: list[CallDiagnostic],
    count: int,
    outcome: str,
    receiver_relation: str | None = None,
) -> list[CallSite]:
    candidates: list[CallDiagnostic] = []
    for diagnostic in diagnostics:
        receiver = diagnostic["normalInference"]["receiver"]
        result = diagnostic["twoPhaseInference"]
        if (
            (receiver_relation is None or receiver["relation"] == receiver_relation)
            and result["result"] == "success"
            and result["outcome"] == outcome
        ):
            candidates.append(diagnostic)

    return [
        {
            "inputPath": diagnostic["_inputPath"],
            "location": diagnostic["_location"],
            "diagnostic": diagnostic,
        }
        for diagnostic in random.sample(candidates, min(count, len(candidates)))
    ]


def inapplicable_diagnostics(diagnostics: list[CallDiagnostic]) -> list[CallDiagnostic]:
    return [
        diagnostic
        for diagnostic in diagnostics
        if (result := diagnostic["twoPhaseInference"])["result"] == "success" and result["outcome"] == "inapplicable"
    ]


def print_top_over_approximated_callables(summaries: list[SummaryDiagnostic], count: int) -> None:
    totals: Counter[str] = Counter()
    over_approximated: Counter[str] = Counter()
    signature_totals: Counter[tuple[str, str]] = Counter()
    signature_over_approximated: Counter[tuple[str, str]] = Counter()
    for diagnostic in summaries:
        summary = diagnostic["summary"]
        callable_id = diagnostic["callableId"]
        signature_key = callable_id, diagnostic["signature"]
        totals[callable_id] += summary["totalCalls"]
        over_approximated[callable_id] += summary["receiverRelations"].get("over_approximated", 0)
        signature_totals[signature_key] += summary["totalCalls"]
        signature_over_approximated[signature_key] += summary["receiverRelations"].get("over_approximated", 0)

    rows = [
        (matching * 100.0 / totals[callable_id], matching, totals[callable_id], callable_id)
        for callable_id, matching in over_approximated.items()
        if matching > 0 and totals[callable_id] > 0
    ]
    rows.sort(key=lambda row: (-row[0], -row[1], row[3]))

    print("\nTop callables by percentage of over-approximated receivers:")
    if not rows:
        print("  (none)")
    for percentage, matching, total, callable_id in rows[:count]:
        print(f"  {percentage:7.2f}%  {matching:6}/{total:<6}  {callable_id}")

    signature_rows = [
        (matching * 100.0 / signature_totals[key], matching, signature_totals[key], key)
        for key, matching in signature_over_approximated.items()
        if matching > 0 and signature_totals[key] > 0
    ]
    signature_rows.sort(key=lambda row: (-row[0], -row[1], row[3]))

    print("\nTop signatures by percentage of over-approximated receivers:")
    if not signature_rows:
        print("  (none)")
    for percentage, matching, total, (callable_id, signature) in signature_rows[:count]:
        print(f"  {percentage:7.2f}%  {matching:6}/{total:<6}  {callable_id} {signature}")


def as_call_site(diagnostic: CallDiagnostic) -> CallSite:
    return {
        "inputPath": diagnostic["_inputPath"],
        "location": diagnostic["_location"],
        "diagnostic": diagnostic,
    }


def print_samples(heading: str, samples: list[CallSite]) -> None:
    print(f"\n{heading}:")
    if not samples:
        print("  (none)")
    for index, sample in enumerate(samples, start=1):
        diagnostic = sample["diagnostic"]
        receiver = diagnostic["normalInference"]["receiver"]
        normal_types = diagnostic["normalInference"]["inferredTypes"]
        result = diagnostic["twoPhaseInference"]
        assert result["result"] == "success"
        inferred_types = result["inferredTypes"]
        rendered_normal_types = ", ".join(f"{name}={type_}" for name, type_ in normal_types.items())
        rendered_types = ", ".join(f"{name}={type_}" for name, type_ in inferred_types.items())
        print(f"\n  {index}. {sample['location']}")
        print(f"     Input: {sample['inputPath']}")
        print(f"     Callable: {diagnostic['callableId']} {diagnostic['signature']}")
        print(f"     Receiver: {receiver['actualType']} -> {receiver['inferredType']}")
        print(f"     Normal types: {rendered_normal_types}")
        print(f"     Two-phase types: {rendered_types}")


def print_inapplicable_csv(
    summaries: list[SummaryDiagnostic],
) -> None:
    total_calls: Counter[tuple[str, str]] = Counter()
    inapplicable_calls: Counter[tuple[str, str]] = Counter()
    for diagnostic in summaries:
        callable_id = diagnostic["callableId"]
        key = callable_id, diagnostic["signature"]
        total_calls[key] += diagnostic["summary"]["totalCalls"]
        inapplicable_calls[key] += diagnostic["summary"]["inapplicableCalls"]

    rows: list[tuple[float, int, str, str]] = []
    for (callable_id, signature), total in total_calls.items():
        if total == 0:
            continue
        count = inapplicable_calls[callable_id, signature]
        percentage = count * 100.0 / total
        rows.append((percentage, total, callable_id, signature))

    writer = csv.writer(sys.stdout, lineterminator="\n")
    writer.writerow(("callableId", "signature", "inapplicablePercent", "totalCalls"))
    for percentage, total, callable_id, signature in sorted(rows, key=lambda row: (-row[0], row[2], row[3])):
        writer.writerow((callable_id, signature, f"{percentage:.6f}", total))


def print_signature_summary_csv(summaries: list[SummaryDiagnostic]) -> None:
    total_calls: Counter[tuple[str, str]] = Counter()
    constructor_approximated_calls: Counter[tuple[str, str]] = Counter()
    parameter_approximated_calls: Counter[tuple[str, str]] = Counter()
    constructor_and_parameter_approximated_calls: Counter[tuple[str, str]] = Counter()
    inapplicable_calls: Counter[tuple[str, str]] = Counter()

    for diagnostic in summaries:
        key = diagnostic["callableId"], diagnostic["signature"]
        summary = diagnostic["summary"]
        total_calls[key] += summary["totalCalls"]
        constructor_approximated_calls[key] += summary["receiverTypeConstructorApproximatedCalls"]
        parameter_approximated_calls[key] += summary["receiverTypeParameterApproximatedCalls"]
        constructor_and_parameter_approximated_calls[key] += summary[
            "receiverTypeConstructorAndParameterApproximatedCalls"
        ]
        inapplicable_calls[key] += summary["inapplicableCalls"]

    writer = csv.writer(sys.stdout, lineterminator="\n")
    writer.writerow(
        (
            "callableId",
            "signature",
            "totalCalls",
            "receiverTypeConstructorApproximatedCalls",
            "receiverTypeParameterApproximatedCalls",
            "receiverTypeConstructorAndParameterApproximatedCalls",
            "memberExtensionInferenceInapplicable",
            "memberExtensionInferenceInapplicableFraction",
        )
    )
    rows = [
        (inapplicable_calls[key] / total, total, key)
        for key, total in total_calls.items()
        if total > 0
    ]
    rows.sort(key=lambda row: (-row[0], -row[1], row[2]))
    for inapplicable_fraction, _total, (callable_id, signature) in rows:
        key = callable_id, signature
        writer.writerow(
            (
                callable_id,
                signature,
                total_calls[key],
                constructor_approximated_calls[key],
                parameter_approximated_calls[key],
                constructor_and_parameter_approximated_calls[key],
                inapplicable_calls[key],
                f"{inapplicable_fraction:.6f}",
            )
        )


def print_detailed_call_sites(heading: str, call_sites: list[CallSite]) -> None:
    print(f"{heading}:")
    if not call_sites:
        print("  (none)")
        return

    for index, call_site in enumerate(call_sites, start=1):
        diagnostic = {
            key: value for key, value in call_site["diagnostic"].items() if key not in ("_inputPath", "_location")
        }
        detail = {
            "inputPath": call_site["inputPath"],
            "location": call_site["location"],
            "diagnostic": diagnostic,
        }
        print(f"\n{index}.")
        print(json.dumps(detail, indent=2, sort_keys=True))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "inputs",
        nargs="+",
        type=Path,
        help="YAML result paths, or filenames inside the analysis directory.",
    )
    parser.add_argument("--json", action="store_true", help="Print the summary as JSON.")
    parser.add_argument(
        "--inapplicable-csv",
        action="store_true",
        help="Output CSV rows with per-signature call totals and inapplicable percentages.",
    )
    parser.add_argument(
        "--signature-summary-csv",
        action="store_true",
        help="Output aggregate call, approximation, and inapplicability counts for every callable signature.",
    )
    parser.add_argument(
        "--sample-over-approximated-successful",
        type=int,
        metavar="N",
        default=0,
        help="Output N random successful calls whose receiver was over-approximated.",
    )
    parser.add_argument(
        "--sample-inapplicable",
        type=int,
        metavar="N",
        default=0,
        help="Output N random calls whose two-phase inference result was inapplicable.",
    )
    parser.add_argument(
        "--sample-failed",
        type=int,
        metavar="N",
        default=0,
        help="Output N random calls whose receiver-phase inference failed.",
    )
    parser.add_argument(
        "--top-inapplicable-callables",
        type=int,
        metavar="N",
        default=0,
        help="Output the top N callable IDs by number of inapplicable two-phase results.",
    )
    parser.add_argument(
        "--top-over-approximated-callables",
        type=int,
        metavar="N",
        default=0,
        help="Output the top N callable IDs by percentage of calls with over-approximated receivers.",
    )
    parser.add_argument(
        "--inapplicable-details",
        metavar="CALLABLE_ID",
        help="Output the complete diagnostic for every inapplicable call with one callable ID.",
    )
    args = parser.parse_args(namespace=Arguments())
    if args.sample_over_approximated_successful < 0:
        parser.error("--sample-over-approximated-successful must be non-negative")
    if args.sample_inapplicable < 0:
        parser.error("--sample-inapplicable must be non-negative")
    if args.sample_failed < 0:
        parser.error("--sample-failed must be non-negative")
    if args.top_inapplicable_callables < 0:
        parser.error("--top-inapplicable-callables must be non-negative")
    if args.top_over_approximated_callables < 0:
        parser.error("--top-over-approximated-callables must be non-negative")
    text_report_requested = (
        args.sample_over_approximated_successful
        or args.sample_inapplicable
        or args.sample_failed
        or args.top_inapplicable_callables
        or args.top_over_approximated_callables
        or args.inapplicable_details
    )
    if args.json and (text_report_requested or args.inapplicable_csv or args.signature_summary_csv):
        parser.error("--json cannot be combined with other report modes")
    if args.inapplicable_csv and args.signature_summary_csv:
        parser.error("--inapplicable-csv and --signature-summary-csv cannot be combined")
    if (args.inapplicable_csv or args.signature_summary_csv) and text_report_requested:
        parser.error("CSV modes cannot be combined with sampling or call-site report options")

    input_paths = [resolve_input_path(path) for path in args.inputs]
    diagnostics, diagnostic_summaries, malformed = load_diagnostics(input_paths)
    summary = summarize(diagnostics, diagnostic_summaries, malformed)
    successful_samples = sample_analyzed_calls(
        diagnostics,
        args.sample_over_approximated_successful,
        outcome="successful",
        receiver_relation="over_approximated",
    )
    inapplicable_samples = sample_analyzed_calls(diagnostics, args.sample_inapplicable, outcome="inapplicable")
    failed_samples = sample_analyzed_calls(diagnostics, args.sample_failed, outcome="failed")
    failures = inapplicable_diagnostics(diagnostics)
    if args.json:
        print(json.dumps(summary, indent=2, sort_keys=True))
        return
    if args.inapplicable_csv:
        print_inapplicable_csv(diagnostic_summaries)
        return
    if args.signature_summary_csv:
        print_signature_summary_csv(diagnostic_summaries)
        return
    if args.inapplicable_details:
        call_sites = [
            as_call_site(diagnostic) for diagnostic in failures if diagnostic["callableId"] == args.inapplicable_details
        ]
        print_detailed_call_sites(
            f"Inapplicable calls for {args.inapplicable_details}",
            call_sites,
        )
        return

    print(f"Analyzed calls: {summary['analyzedCallCount']}")
    print(f"Call diagnostics: {summary['callDiagnosticCount']}")
    print(f"Summary diagnostics: {summary['summaryDiagnosticCount']}")
    print(f"Malformed payloads: {summary['malformedCount']}")
    for heading, values in (
        ("Result kinds", summary["resultKinds"]),
        ("Outcomes", summary["outcomes"]),
        ("Receiver relations", summary["receiverRelations"]),
        ("Receiver approximations", summary["receiverApproximations"]),
        ("Error reasons", summary["errorReasons"]),
        ("Top callables", summary["topCallables"]),
        ("Top signatures", summary["topSignatures"]),
    ):
        print(f"\n{heading}:")
        if not values:
            print("  (none)")
        for name, count in values.items():
            print(f"  {name}: {count}")

    if args.sample_over_approximated_successful:
        print_samples("Random over-approximated, successful calls", successful_samples)
    if args.sample_inapplicable:
        print_samples("Random inapplicable calls", inapplicable_samples)
    if args.sample_failed:
        print_samples("Random receiver-inference failures", failed_samples)
    if args.top_inapplicable_callables:
        counts = Counter(diagnostic["callableId"] for diagnostic in failures)
        signature_counts = Counter((diagnostic["callableId"], diagnostic["signature"]) for diagnostic in failures)
        print("\nTop callables by inapplicable two-phase results:")
        if not counts:
            print("  (none)")
        for callable_id, count in counts.most_common(args.top_inapplicable_callables):
            print(f"  {count:6}  {callable_id}")
        print("\nTop signatures by inapplicable two-phase results:")
        if not signature_counts:
            print("  (none)")
        for (callable_id, signature), count in signature_counts.most_common(args.top_inapplicable_callables):
            print(f"  {count:6}  {callable_id} {signature}")
    if args.top_over_approximated_callables:
        print_top_over_approximated_callables(diagnostic_summaries, args.top_over_approximated_callables)


if __name__ == "__main__":
    main()
