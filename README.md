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

TODO

## Example usage in Frontend

An example Vue app is included in examples/tauri-plugin-hid-vue-example
```ts
import { HidDevice, enumerate } from "@redfernelec/tauri-plugin-hid-api";

let myDevice: HidDevice;

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