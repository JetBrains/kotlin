export interface KotlinTestRunner {
    suite(name: string, isIgnored: boolean, fn: () => void): void

    test(name: string, isIgnored: boolean, fn: () => void): void
}