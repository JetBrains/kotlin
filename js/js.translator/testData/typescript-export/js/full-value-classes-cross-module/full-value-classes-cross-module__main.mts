import { LibPoint, createMainPoint, echoLibPoint } from "./full-value-classes-cross-module-lib_v5.mjs";

export function box(): string {
    const point: LibPoint = createMainPoint(20, 22);
    if (point.x !== 20 || point.y !== 22) {
        return `FAIL createMainPoint`;
    }

    const echoedPoint = echoLibPoint(point);
    if (echoedPoint.x !== 20 || echoedPoint.y !== 22) {
        return `FAIL echoLibPoint`;
    }

    const constructedPoint = new LibPoint(30, 12);
    const echoedConstructedPoint = echoLibPoint(constructedPoint);
    if (echoedConstructedPoint.x !== 30 || echoedConstructedPoint.y !== 12) {
        return `FAIL constructed LibPoint`;
    }

    return "OK";
}
