# Measurements of KLIB DAG generation

* Date & time: 2026-10-12T21:26:47.920773Z
* OS: Mac OS X (26.5.2), aarch64
* CPU cores: 12
* Max heap: 3641 MiB

### Target: macos_arm64
| Test name | no indices<br/>(median time) | with indices<br/>(median time) |
|---|---|---|
| stdlib and platform libraries only (no roots) | 359.79 us<br/>100% | 363.38 us<br/>↑ 101.00% |
| stdlib and platform libraries only (roots = stdlib + Foundation) | 663.62 ms<br/>100% | 5.08 ms<br/>↓ 0.77% |
| stdlib and platform libraries (roots = 20 + 0 user libs) | 11.60 ms<br/>100% | 4.92 ms<br/>↓ 42.45% |
| stdlib and platform libraries (roots = 15 + 5 user libs) | 58.72 ms<br/>100% | 5.67 ms<br/>↓ 9.66% |
| stdlib and platform libraries (roots = 10 + 10 user libs) | 121.59 ms<br/>100% | 8.14 ms<br/>↓ 6.69% |
| stdlib and platform libraries (roots = 100 + 0 user libs) | 59.38 ms<br/>100% | 25.67 ms<br/>↓ 43.22% |
| stdlib and platform libraries (roots = 75 + 25 user libs) | 774.13 ms<br/>100% | 26.02 ms<br/>↓ 3.36% |
| stdlib and platform libraries (roots = 50 + 50 user libs) | 3.35 s<br/>100% | 57.94 ms<br/>↓ 1.73% |
