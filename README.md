# Tauri Plugin HID

Tauri plugin to provide access to USB HID devices.

Currently uses hidapi-rusb as it is able to compile hidapi on Android as part of a Tauri project without errors.
(Android not implemented yet).

⚠️ **Warning: Work in Progress** ⚠️

**Features:**

*   Enumerate devices
*   Open multiple devices simultaneously
*   Read and write input and output reports

**Limitations:**

*   Does not enumerate devices on Android yet
*   Feature reports not supported yet
*   Currently only tested on macOS

## Installation

Install the plugin with cargo:
```sh
cd src-tauri
cargo add tauri-plugin-hid
```

Alternatively add the dependency directly to Cargo.toml:
```toml
[dependencies]
tauri-plugin-hid = "0.1.1"
```

Install the ts/js api:
```sh
npm add @redfernelec/tauri-plugin-hid-api
```

Add the plugin to ```src-tauri/src/lib.rs```, for example:
```rust
tauri::Builder::default()
    .plugin(tauri_plugin_opener::init())
    .plugin(tauri_plugin_hid::init())   // Register hid plugin
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
```

Add permisions to ```src-tauri/capabilities/default.json```:
```json
"permissions": [
    "core:default",
    "opener:default",
    "hid:default"
]
```

## Example usage in Frontend

An example Vue app is included in examples/tauri-plugin-hid-vue-example
```typescript
import { HidDevice, enumerate } from "@redfernelec/tauri-plugin-hid-api";

let myDevice: HidDevice | null = null;

// Enumerate devices and find one based on product string
let devices = await enumerate();
for (const device of devices) {
    if (device.productString === "My Device") {
        myDevice = device;
        break;
    }
}

if(myDevice) {
    await myDevice.open();
    await myDevice.write(new Uint8Array([0x00, 0x00]));
    let data = await myDevice.read(2);
    await myDevice.close();
}
```