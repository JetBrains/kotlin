import {KotlinTestRunner} from "./src/KotlinTestRunner";

const kotlin_test = require('kotlin-test');

const nothingTest: KotlinTestRunner = {
    suite(name: string, isIgnored: boolean, fn: () => void): void {
        // do nothing
    },
    test(name: string, isIgnored: boolean, fn: () => void): void {
        // do nothing
    }
};

kotlin_test.setAdapter(nothingTest);