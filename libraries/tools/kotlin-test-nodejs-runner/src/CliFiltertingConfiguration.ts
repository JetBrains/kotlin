import {KotlinTestRunner} from "./KotlinTestRunner";
import {
    allTest,
    CompositeTestFilter,
    KotlinTestsFilter,
    newKotlinTestsFilter,
    runWithFilter
} from "./KotlinTestsFilter";
import {flatMap, println, pushIfNotNull} from "./utils";

export function configureFiltering(
    runner: KotlinTestRunner,
    includeWildcards: string[],
    excludeWildcards: string[]
): KotlinTestRunner {
    const include: KotlinTestsFilter[] = [];
    const exclude: KotlinTestsFilter[] = [];

    function collectWildcards(
        value: string[],
        positive: KotlinTestsFilter[],
        negative: KotlinTestsFilter[]
    ) {
        flatMap(value, (t: string) => t.split(','))
            .map(t => {
                if (t.length && t[0] == '!') {
                    pushIfNotNull(negative, newKotlinTestsFilter(t.substring(1)))
                } else {
                    pushIfNotNull(positive, newKotlinTestsFilter(t))
                }
            })
    }

    collectWildcards(includeWildcards, include, exclude);
    collectWildcards(excludeWildcards, exclude, include);

    if (include.length == 0 && exclude.length == 0) {
        return runner
    } else {
        if (include.length == 0) {
            include.push(allTest)
        }

        const filter = new CompositeTestFilter(include, exclude);

        if (DEBUG) {
            println(filter.toString());
        }

        return runWithFilter(runner, filter)
    }
}
