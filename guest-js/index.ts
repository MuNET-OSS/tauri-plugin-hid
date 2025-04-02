import { invoke } from '@tauri-apps/api/core'

type HidDeviceInfo = {
  path: string ;
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
  id: string = '';  // UUID used to match device to the connected device in rust
  path: string = '';
  vendorId: number = 0;
  productId: number = 0;
  serialNumber: string = '';
  releaseNumber: number = 0;
  manufacturerString: string = '';
  productString: string = '';

  // TODO: consider using path first then vid/pid
  async open(): Promise<void> {
    this.id = await invoke('plugin:hid|open', {
      vendorId: this.vendorId,
      productId: this.productId,
    });
  }

  async close(): Promise<void> {
    return await invoke('plugin:hid|close', {
      id: this.id,
    });
  }

  async read(length: number): Promise<ArrayBuffer> {
    return await invoke('plugin:hid|read', {
      id: this.id,
      size: length,
    });
  }

  async write(data: ArrayBuffer): Promise<void> {
    return await invoke('plugin:hid|write', {
      id: this.id,
      data,
    });
  }
};