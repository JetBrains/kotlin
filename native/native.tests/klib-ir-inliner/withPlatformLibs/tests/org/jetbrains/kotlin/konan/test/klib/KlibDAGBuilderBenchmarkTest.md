# Measurements of KLIB DAG generation

* Date & time: 2026-10-14T09:58:06.692566Z
* OS: Mac OS X (26.5.2), aarch64
* CPU cores: 12
* Max heap: 3641 MiB

### Target: macos_arm64
| Test name | no-indices/d=0.10<br/>(median time) | with-indices/d=0.10<br/>(median time) | with-indices/d=0.50<br/>(median time) | with-indices/d=0.90<br/>(median time) |
|---|---|---|---|---|
| stdlib and platform libs only (no roots) | 420.83 us<br/>100% | 399.21 us<br/>↓ 94.86% | 329.88 us<br/>↓ 78.39% | 280.04 us<br/>↓ 66.54% |
| stdlib and platform libs only (roots = stdlib + Foundation) | 636.19 ms<br/>100% | 4.12 ms<br/>↓ 0.65% | 4.19 ms<br/>↓ 0.66% | 4.10 ms<br/>↓ 0.65% |
| stdlib, platform and user libs<br/>roots = 20 regular libs | 11.38 ms<br/>100% | 4.95 ms<br/>↓ 43.54% | 5.09 ms<br/>↓ 44.75% | 4.84 ms<br/>↓ 42.58% |
| stdlib, platform and user libs<br/>roots = 15 regular + 5 C-interop libs | 48.99 ms<br/>100% | 4.98 ms<br/>↓ 10.16% | 4.96 ms<br/>↓ 10.12% | 5.29 ms<br/>↓ 10.79% |
| stdlib, platform and user libs<br/>roots = 10 regular + 10 C-interop libs | 119.60 ms<br/>100% | 8.13 ms<br/>↓ 6.80% | 4.85 ms<br/>↓ 4.06% | 4.89 ms<br/>↓ 4.09% |
| stdlib, platform and user libs<br/>roots = 100 regular libs | 58.14 ms<br/>100% | 24.71 ms<br/>↓ 42.50% | 25.40 ms<br/>↓ 43.69% | 26.42 ms<br/>↓ 45.44% |
| stdlib, platform and user libs<br/>roots = 75 regular + 25 C-interop libs | 253.42 ms<br/>100% | 22.98 ms<br/>↓ 9.07% | 25.16 ms<br/>↓ 9.93% | 25.31 ms<br/>↓ 9.99% |
| stdlib, platform and user libs<br/>roots = 50 regular + 50 C-interop libs | 771.47 ms<br/>100% | 26.41 ms<br/>↓ 3.42% | 24.87 ms<br/>↓ 3.22% | 24.76 ms<br/>↓ 3.21% |
