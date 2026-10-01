### KlibDAGBuilder Benchmarking Results (Apple M2 Max)

The typical library contains ~2k declarations. The median duration of Klib dependency DAG computation is show in cells.

| Mode \ Number of user libraries | regular: 20<br/>c-interop: 0 | regular: 19<br/>c-interop: 1 | regular: 18<br/>c-interop: 2 | regular: 15<br/>c-interop: 5 | regular: 10<br/>c-interop: 10 | regular: 100<br/>c-interop: 0 | regular: 99<br/>c-interop: 1 | regular: 98<br/>c-interop: 2 | regular: 95<br/>c-interop: 5 | regular: 90<br/>c-interop: 10 |
|---|---|---|---|---|---|---|---|---|---|---|
| Probability | High | High | High | Low | Extremely low | High | High | High | Low | Extremely low |
| Signature indices are not used. | 11 ms | 25 ms | 27 ms | 70 ms | 155 ms | 57 ms | 59 ms | 66 ms | 96 ms | 206 ms |
| Signature indices are used only for libraries from the distribution.<br/>User libraries are without indices (aka "old" libraries). | 10 ms | 24 ms | 26 ms | 68 ms | 154 ms | 54 ms | 56 ms | 64 ms | 94 ms | 206 ms |
| Signature indices are used everywhere.<br/>User libraries do not have their own indices, so "external" cached indices are used for them. | 8 ms | 8 ms | 8 ms | 9 ms | 15 ms | 42 ms | 43 ms | 44 ms | 41 ms | 43 ms |
| Signature indices are used everywhere.<br/>User libraries have their own indices (aka "new" libraries). | 5 ms | 5 ms | 5 ms | 6 ms | 9 ms | 25 ms | 24 ms | 24 ms | 23 ms | 24 ms |
