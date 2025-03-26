# Tauri Plugin hid

Tauri plugin to provide access to USB HID devices.

Currently uses hidapi-rusb as it is able to compile hidapi on Android as part of a Tauri project without errors.

⚠️ **Warning: Work in Progress** ⚠️

**Features:**

*   Basic test functions to open, read, and write to HID devices.

**Limitations:**

*   No error handling implemented yet
*   Only connects to one device at a time (i.e. plugin manages single current device)
*   API will change in future versions
*   Does not enumerate devices on Android yet
*   Only tested on macOS

