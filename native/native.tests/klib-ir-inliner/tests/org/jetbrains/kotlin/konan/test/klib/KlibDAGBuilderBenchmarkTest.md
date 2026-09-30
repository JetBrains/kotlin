# Measurements of KLIB DAG generation

* Date & time: 2026-10-16T18:38:23.618998Z
* OS: Mac OS X (26.5.2), aarch64
* CPU cores: 12
* Max heap: 3989 MiB

### Target: macos_arm64
| Test name | no-indices/d=0.50<br/>(median time) | own-indices-for-dist-no-indices-for-others/d=0.50<br/>(median time) | own-indices-for-dist-external-indices-for-others/d=0.50<br/>(median time) | own-indices-everywhere/d=0.50<br/>(median time) |
|---|---|---|---|---|
| stdlib and platform libs only (no roots) | 402.71 us<br/>100% | 415.21 us<br/>↑ 103.10% | 421.04 us<br/>↑ 104.55% | 278.04 us<br/>↓ 69.04% |
| stdlib and platform libs only (roots = stdlib + Foundation) | 642.84 ms<br/>100% | 4.23 ms<br/>↓ 0.66% | 4.34 ms<br/>↓ 0.68% | 4.27 ms<br/>↓ 0.66% |
| stdlib, platform and user libs<br/>roots = 20 regular libs | 13.21 ms<br/>100% | 11.48 ms<br/>↓ 86.89% | 8.36 ms<br/>↓ 63.29% | 4.89 ms<br/>↓ 37.02% |
| stdlib, platform and user libs<br/>roots = 19 regular + 1 C-interop libs | 19.63 ms<br/>100% | 19.58 ms<br/>↓ 99.76% | 8.78 ms<br/>↓ 44.75% | 4.89 ms<br/>↓ 24.89% |
| stdlib, platform and user libs<br/>roots = 18 regular + 2 C-interop libs | 21.42 ms<br/>100% | 22.30 ms<br/>↑ 104.14% | 8.45 ms<br/>↓ 39.47% | 4.82 ms<br/>↓ 22.50% |
| stdlib, platform and user libs<br/>roots = 100 regular libs | 58.98 ms<br/>100% | 59.05 ms<br/>↑ 100.12% | 46.08 ms<br/>↓ 78.13% | 25.38 ms<br/>↓ 43.03% |
| stdlib, platform and user libs<br/>roots = 95 regular + 5 C-interop libs | 75.90 ms<br/>100% | 75.17 ms<br/>↓ 99.03% | 45.76 ms<br/>↓ 60.29% | 25.15 ms<br/>↓ 33.14% |
| stdlib, platform and user libs<br/>roots = 90 regular + 10 C-interop libs | 96.12 ms<br/>100% | 97.33 ms<br/>↑ 101.26% | 46.42 ms<br/>↓ 48.30% | 25.46 ms<br/>↓ 26.49% |
| stdlib, platform and user libs<br/>roots = 1000 regular libs | 1.09 s<br/>100% | 1.10 s<br/>↑ 100.07% | 887.23 ms<br/>↓ 81.06% | 243.55 ms<br/>↓ 22.25% |
| stdlib, platform and user libs<br/>roots = 990 regular + 10 C-interop libs | 1.20 s<br/>100% | 1.21 s<br/>↑ 100.42% | 890.14 ms<br/>↓ 73.91% | 244.11 ms<br/>↓ 20.27% |
| stdlib, platform and user libs<br/>roots = 980 regular + 20 C-interop libs | 1.28 s<br/>100% | 1.28 s<br/>↑ 100.36% | 886.88 ms<br/>↓ 69.52% | 245.29 ms<br/>↓ 19.23% |
