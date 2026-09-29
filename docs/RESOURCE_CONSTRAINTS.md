# Resource Constraints & Target Hardware Profile

## Target Hardware Specifications

* **Device**: Tecno Camon i
* **SoC**: MediaTek Helio P23 (MT6763V)
* **RAM**: 4 GB
* **OS**: Android 8.1 (API Level 27) / HiOS OEM environment

## Operational Constraints & Mitigation Rules

1. **Memory Bounds**:
   * Maximum Heap Allocation: Designed for low memory foot-print (< 64 MB operational heap).
   * No bitmap screenshot caches retained in memory.
   * Ring-buffer logger capped at 100 entries (`Logger.kt`).
2. **CPU & Rendering**:
   * No heavy continuous background loops, continuous polling, or recursive DOM hierarchy crawling.
   * Standard Android XML Views without heavy animations or WebViews.
3. **Storage & I/O**:
   * Bounded log history to prevent storage exhaustion on slow eMMC storage.
   * Minimal I/O writes during accessibility observation.
