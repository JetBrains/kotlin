import {EmptyKotlinTestRunner} from "./src/EmptyKotlinTestRunner";

(globalThis as any).kotlinTest = {
    adapterTransformer: () => new EmptyKotlinTestRunner()
}