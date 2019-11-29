import {KotlinTestRunner} from "./src/KotlinTestRunner";

const kotlin_test = require('kotlin-test');

const nothingTest: KotlinTestRunner = {
    suite(name: string, isIgnored: boolean, fn: () => void): void {
        console.error("suite", name)
    },
    test(name: string, isIgnored: boolean, fn: () => void): void {
        console.error("test", name)

    }
};

kotlin_test.setAdapter(nothingTest);