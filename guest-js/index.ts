import { invoke } from '@tauri-apps/api/core'

// TODO: Add documentation

type HidDeviceInfo = {
  path: string;
  vendorId: number;
  productId: number;
  serialNumber: string;
  releaseNumber: number;
  manufacturerString: string;
  productString: string;
};

export async function enumerate(): Promise<HidDevice[]> {
  const infoList = await invoke('plugin:hid|enumerate', {});
  const devices: HidDevice[] = [];
  for (const info of infoList as HidDeviceInfo[]) {
    const device = new HidDevice();
    Object.assign(device, info);
    devices.push(device);
  }
  return devices;
}

export class HidDevice {
  path: string = '';
  vendorId: number = 0;
  productId: number = 0;
  serialNumber: string = '';
  releaseNumber: number = 0;
  manufacturerString: string = '';
  productString: string = '';
  isOpen: boolean = false;

  async open(): Promise<void> {
    await invoke('plugin:hid|open', {
      path: this.path,
    });
    this.isOpen = true;
  }

  async close(): Promise<void> {
    this.isOpen = false;
    await invoke('plugin:hid|close', {
      path: this.path,
    });
  }

  async read(timeout: number = 0): Promise<ArrayBuffer> {
    return await invoke('plugin:hid|read', {
      path: this.path,
      timeout: timeout
    });
  }

  async write(data: ArrayBuffer): Promise<void> {
    await invoke('plugin:hid|write', {
      path: this.path,
      data,
    });
  }
};