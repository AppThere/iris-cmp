# Hardware matrix

Filled in during Phase 0 (reference devices) and re-run at every gate.

| Role | Device | OS | Pen | Notes |
| --- | --- | --- | --- | --- |
| Low-end Android phone | TBD | Android 9+ | none or S Pen | |
| Mid-range Android tablet | Samsung Galaxy Tab S10 Lite (available 2026-10-04) | Android (version to record) | S Pen | reference for 60 fps budget; S9 capability report records which axes the pen reports |
| iPad | TBD | iPadOS | Apple Pencil | |
| Windows 11 laptop | TBD | Windows 11 | Surface Pen or Wacom | |
| Linux machine | Development machine, Ubuntu 26.04 (available) | KDE Plasma, Wayland | none yet | S3 is partial until a tablet is attached (D-022) |
| Mac | MacBook Air M1 (available 2026-10-04) | macOS (version to record) | none | Builds and runs iOS (simulator) and macOS; no macOS pen evidence without a tablet |
| ChromeOS | TBD | ChromeOS | USI | |

## Manual checklist per gate

- [ ] Pressure curve feels right; light touch makes a visible mark
- [ ] Tilt and azimuth change the brush as configured
- [ ] Hover cursor appears where supported
- [ ] Eraser end switches tool
- [ ] Pen buttons and Apple Pencil double-tap/squeeze run bound actions
- [ ] Palm rejection: resting a hand does not draw
- [ ] Two-finger pan/zoom/rotate while the pen is in range
- [ ] Stroke latency with and without prediction recorded
- [ ] Save, quit, reopen; recovery after force-kill
