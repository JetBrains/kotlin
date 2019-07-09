export interface KotlinTestRunner {
    suite(name: string, isIgnored: boolean, fn: () => void): void

    test(name: string, isIgnored: boolean, fn: () => void): void
}

export const directRunner: KotlinTestRunner = {
    suite(name: string, isIgnored: boolean, fn: () => void): void {
        if (!isIgnored) fn()
    },
    test(name: string, isIgnored: boolean, fn: () => void): void {
        if (!isIgnored) fn()
    }
};