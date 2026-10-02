# LocalAgent Foundation Testing Strategy (Phase 2.4.1)

## Overview
Phase 2.4.1 updates the Foundation Test Registry and Test Runner contracts to align 100% with production command syntax, negative test semantics, and special access blocking conditions.

## Key Test Contract Updates
1. **Notes Command Syntax**: Updated from `note append` to production supported syntax `note down <content>`.
2. **Brightness Status vs. Set**: `brightness status` is read-only and never validates setter percentages. `brightness.set <percentage>` validates 0..100% bounds (`brightness 128` correctly fails with `INVALID_ARGUMENT`).
3. **Sensor Enumeration**: `sensor.list` uses Android `SensorManager` to dynamically enumerate available hardware sensors on device without hard-coding or continuous listener polling.
4. **Negative Test Semantics**: Tests marked `TestType.NEGATIVE` evaluate to `PASSED` when the production pipeline correctly rejects invalid arguments with the expected error code (`INVALID_ARGUMENT`, `NO_SENSOR`, `APP_NOT_FOUND`, `DIVISION_BY_ZERO`, etc.).
5. **Special Access & Capability Blocking**: Missing `Notification Policy Access`, `WRITE_SETTINGS`, or direct Bluetooth/Wi-Fi OS restrictions evaluate to `BLOCKED` with explicit reasons rather than claiming implementation failure.
6. **Dynamic Sensor Discovery & Validation**: Dynamic sensor discovery enumerates all sensors via `SensorManager.getSensorList(Sensor.TYPE_ALL)` and tests single-sensor sequential registration/sampling (`SENSOR_DISCOVERY_ALL`, `SENSOR_REGISTER_ALL_SUPPORTED`).
7. **Multi-Torch Flashlight Control**: Flashlight tests cover dynamic multi-torch controls (`flashlight status`, `flashlight front`, `flashlight back`, `flashlight both`, `flashlight off`) and negative target validation (`flashlight invalid_target`).
8. **Phase 3.2 Target Resolution & Composite Plan Contracts**: Read-only target resolution (`P3.2-TGT-001`..`010`) and composite plan execution models (`P3.2-COMP-001`..`005`) are tested in isolated Phase 3.2 test suites.
