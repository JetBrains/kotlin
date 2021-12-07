export interface KotlinTestRunner {
    suite(name: string, isIgnored: boolean, fn: () => void): void

    test(name: string, isIgnored: boolean, fn: () => void): void

    beforeEach(name: string, fn: () => void): void

    afterEach(name: string, fn: () => void): void
}
