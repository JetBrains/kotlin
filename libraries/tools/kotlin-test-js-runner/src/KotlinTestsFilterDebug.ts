import {
    allTest,
    CompositeTestFilter,
    ExactFilter,
    RegExpKotlinTestsFilter,
    StartsWithFilter
} from "./KotlinTestsFilter";

declare const DEBUG: boolean;

if (DEBUG) {
    allTest.toString = function () {
        return "(ALL)"
    };

    StartsWithFilter.prototype.toString = function (): string {
        return "(STARTS WITH " + this.prefix + (this.filter ? (" AND " + this.filter) : "") + ")"
    };

    RegExpKotlinTestsFilter.prototype.toString = function (): string {
        return "(REGEXP " + this.regexp + ")"
    };

    ExactFilter.prototype.toString = function (): string {
        return "(EXACT " + this.fqn + ")"
    };

    CompositeTestFilter.prototype.toString = function (): string {
        return this.include.map(it => it.toString()).join(" OR ")
            + "\n NOT (" + this.exclude.map(it => it.toString()).join(" OR ") + ")"
    };
}