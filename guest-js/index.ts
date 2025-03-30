import { invoke } from '@tauri-apps/api/core'

export type DeviceInfo = {
  path: string;
  vendorId: number;
  productId: number;
  serialNumber: string;
  releaseNumber: number;
  manufacturerString: string;
  productString: string;
};

export async function ping(value: string): Promise<string | null> {
  return await invoke<{value?: string}>('plugin:hid|ping', {
    payload: {
      value,
    },
  }).then((r) => (r.value ? r.value : null));
}

export async function deviceList(): Promise<DeviceInfo[]> {
  return await invoke('plugin:hid|device_list');
}

export async function open(vendorId: number, productId: number): Promise<void> {
  return await invoke('plugin:hid|open', {
      vendorId,
      productId,
  });
}

export async function close(): Promise<void> {
  return await invoke('plugin:hid|close');
}

export async function read(size: number): Promise<string> {
  return await invoke('plugin:hid|read', {
    size,
  });
}

export async function write(data: number[]): Promise<void> {
  return await invoke('plugin:hid|write', {
    data,
  });
}