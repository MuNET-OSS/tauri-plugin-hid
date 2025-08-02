import { invoke } from '@tauri-apps/api/core'

/**
 * Internal interface representing HID device information returned by the backend.
 * @internal This type is used internally by the plugin and shouldn't be used directly.
 */
type HidDeviceInfo = {
  /** Unique path identifier for the device */
  /** @remarks The path is used to match devices between frontend and backend */
  path: string;
  /** USB vendor ID (e.g., 0x046D for Logitech) */
  vendorId: number;
  /** USB product ID specific to the device model */
  productId: number;
  /** Device serial number if available */
  serialNumber: string;
  /** Device release/version number */
  releaseNumber: number;
  /** Manufacturer name if available */
  manufacturerString: string;
  /** Product name if available */
  productString: string;
};

/**
 * Enumerates all available HID devices connected to the system.
 * 
 * @returns Promise resolving to an array of HidDevice objects
 * @example
 * ```typescript
 * const devices = await enumerate();
 * console.log(`Found ${devices.length} HID devices`);
 * devices.forEach(device => {
 *   console.log(`${device.manufacturerString} ${device.productString}`);
 * });
 * ```
 */
export async function enumerate(): Promise<HidDevice[]> {
  const infoList = await invoke<HidDevice[]>('plugin:hid|enumerate', {});
  const devices: HidDevice[] = [];
  for (const info of infoList as HidDeviceInfo[]) {
    const device = new HidDevice();
    Object.assign(device, info);
    devices.push(device);
  }
  return devices;
}

/**
 * Class representing a HID device with methods for communication.
 */
export class HidDevice {
  /** Unique path identifier for the device */
  /** @remarks The path is used to match devices between frontend and backend */
  path: string = '';
  /** USB vendor ID (e.g., 0x046D for Logitech) */
  vendorId: number = 0;
  /** USB product ID specific to the device model */
  productId: number = 0;
  /** Device serial number if available */
  /** @remarks The serial number may not be available when enumerating on Android as permissions are required for access */
  serialNumber: string = '';
  /** Device release/version number */
  releaseNumber: number = 0;
  /** Manufacturer name if available */
  manufacturerString: string = '';
  /** Product name if available */
  productString: string = '';
  /** Whether the device is currently open */
  isOpen: boolean = false;

  /**
   * Opens a connection to the HID device.
   * Must be called before read/write operations.
   * 
   * @throws Will throw an error if the device cannot be opened
   * @example
   * ```typescript
   * // Find a specific device by vendor ID and product ID
   * const devices = await enumerate();
   * const myDevice = devices.find(d => d.vendorId === 0x1234 && d.productId === 0x5678);
   * 
   * if (myDevice) {
   *   try {
   *     await myDevice.open();
   *     console.log("Device opened successfully");
   *     
   *     // Perform operations with the device
   *     const data = new Uint8Array([0x01, 0x02, 0x03]);
   *     await myDevice.write(data.buffer);
   *     
   *     // Always close the device when done
   *     await myDevice.close();
   *     console.log("Device closed");
   *   } catch (error) {
   *     console.error("Error:", error);
   *   }
   * } else {
   *   console.error("Device not found");
   * }
   * ```
   */
  async open(): Promise<void> {
    await invoke<void>('plugin:hid|open', {
      path: this.path,
    });
    this.isOpen = true;
  }

  /**
   * Closes the connection to the HID device.
   * Should be called when finished with the device.
   */
  async close(): Promise<void> {
    this.isOpen = false;
    await invoke<void>('plugin:hid|close', {
      path: this.path,
    });
  }

  /**
   * Reads data from the HID device.
   * 
   * @param timeout - Read timeout in milliseconds (-1 = no timeout i.e. wait indefinitely).
   * @returns Promise resolving to an ArrayBuffer containing the read data. If the timeout is reached, the promise will resolve with an empty buffer. (No, it's number[])
   * @throws Will throw an error if reading fails but not if the timeout is reached.
   * @example
   * ```typescript
   * const data = await device.read(100); // 100 milliseconds timeout
   * const view = new Uint8Array(data);
   * console.log("Received data:", Array.from(view));
   * ```
   */
  async read(timeout: number = 0): Promise<number[]> {
    let result = await invoke<number[]>('plugin:hid|read', {
      path: this.path,
      timeout: timeout
    });
    return result
  }

  /**
   * Writes data to the HID device.
   * 
   * @param data - ArrayBuffer containing the data to write
   * @throws Will throw an error if writing fails
   * @example
   * ```typescript
   * // Create a buffer with report ID 0x01 followed by some data
   * const data = new Uint8Array([0x01, 0x02, 0x03, 0x04]);
   * await device.write(data.buffer);
   * console.log("Data written successfully");
   * ```
   * 
   * @remarks
   * For devices with a single report (no report ID), the first byte should be zero.
   * For example: `new Uint8Array([0x00, 0x01, 0x02, 0x03])`
   * The implementation will automatically strip this leading zero for compatibility with the HID API.
   */
  async write(data: number[]): Promise<void> {
    await invoke<void>('plugin:hid|write', {
      path: this.path,
      data,
    });
  }

  async getInputReport(reportId: number, length: number = 65): Promise<number[]> {
    let result = await invoke<number[]>('plugin:hid|get_input_report', {
      path: this.path,
      length,
      reportId
    });
    return result
  }

  async sendOutputReport(data: number[]): Promise<void> {
    await invoke<void>('plugin:hid|send_output_report', {
      path: this.path,
      data,
    });
  }
};