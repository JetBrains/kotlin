# Measurements of KLIB DAG generation

* Date & time: 2026-10-14T09:34:55.290101Z
* OS: Mac OS X (26.5.2), aarch64
* CPU cores: 12
* Max heap: 3641 MiB

### Target: macos_arm64
| Test name | no-indices/d=0.10<br/>(median time) | with-indices/d=0.10<br/>(median time) | with-indices/d=0.50<br/>(median time) | with-indices/d=0.90<br/>(median time) |
|---|---|---|---|---|
| stdlib and platform libs only (no roots) | 532.17 us<br/>100% | 382.17 us<br/>↓ 71.81% | 317.63 us<br/>↓ 59.69% | 300.08 us<br/>↓ 56.39% |
| stdlib and platform libs only (roots = stdlib + Foundation) | 644.31 ms<br/>100% | 4.35 ms<br/>↓ 0.67% | 4.25 ms<br/>↓ 0.66% | 4.47 ms<br/>↓ 0.69% |
| stdlib, platform and user libs<br/>roots = 20 regular libs | 11.11 ms<br/>100% | 4.89 ms<br/>↓ 44.01% | 4.98 ms<br/>↓ 44.82% | 4.99 ms<br/>↓ 44.93% |
| stdlib, platform and user libs<br/>roots = 15 regular + 5 C-interop libs | 50.82 ms<br/>100% | 5.05 ms<br/>↓ 9.94% | 4.97 ms<br/>↓ 9.78% | 5.10 ms<br/>↓ 10.04% |
| stdlib, platform and user libs<br/>roots = 10 regular + 10 C-interop libs | 120.00 ms<br/>100% | 8.16 ms<br/>↓ 6.80% | 4.96 ms<br/>↓ 4.13% | 4.92 ms<br/>↓ 4.10% |
| stdlib, platform and user libs<br/>roots = 100 regular libs | 57.57 ms<br/>100% | 24.74 ms<br/>↓ 42.98% | 25.73 ms<br/>↓ 44.70% | 26.34 ms<br/>↓ 45.76% |
| stdlib, platform and user libs<br/>roots = 75 regular + 25 C-interop libs | 251.81 ms<br/>100% | 23.47 ms<br/>↓ 9.32% | 25.16 ms<br/>↓ 9.99% | 25.78 ms<br/>↓ 10.24% |
| stdlib, platform and user libs<br/>roots = 50 regular + 50 C-interop libs | 770.87 ms<br/>100% | 26.80 ms<br/>↓ 3.48% | 24.75 ms<br/>↓ 3.21% | 25.00 ms<br/>↓ 3.24% |
