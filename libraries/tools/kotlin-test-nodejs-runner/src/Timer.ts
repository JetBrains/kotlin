export interface Timer<T> {
    start(): T

    end(start: T): number
}

export const hrTimer: Timer<[number, number]> = {
    start(): [number, number] {
        return process.hrtime();
    },
    end(start: [number, number]): number {
        const elapsedHr = process.hrtime(start);
        return elapsedHr[0] + (elapsedHr[1] / 1e6); // ns to ms
    }
};

