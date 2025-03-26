use std::vec;
use std::sync::Mutex;

use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};
use hidapi_rusb::HidApi;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<Hid<R>> {
  Ok(Hid {
    app: app.clone(),
    hid_api: Mutex::new(HidApi::new().unwrap()),
    device: Mutex::new(None),
  })
}

/// Access to the hid APIs.
pub struct Hid<R: Runtime> {
  #[allow(dead_code)]
  app: AppHandle<R>,
  hid_api: Mutex<HidApi>,
  device: Mutex<Option<hidapi_rusb::HidDevice>>,
}

impl<R: Runtime> Hid<R> {
  pub fn device_list(&self) -> crate::Result<Vec<crate::DeviceInfo>> {
    let mut devices = vec![];
    let mut hid_api = self.hid_api.lock().unwrap();
    hid_api.refresh_devices().unwrap();
    for device in hid_api.device_list() {
      let device_info = crate::DeviceInfo {
        product_id: device.product_id(),
        vendor_id: device.vendor_id(),
        path: device.path().to_string_lossy().to_string(),
        manufacturer_string: match device.manufacturer_string() {
          Some(manufacturer_string) => {
            Some(manufacturer_string.to_owned())
          },
          None => {
            Some("".to_string())
            // None
          }
        },
        product_string: match device.product_string() {
          Some(product_string) => {
            Some(product_string.to_owned())
          },
          None => {
            Some("".to_string())
            // None
          }
        },
      };
      devices.push(device_info);
    };

    Ok(devices)
  }

  pub fn open(&self, vendor_id: u16, product_id: u16) -> crate::Result<()> {
    let hid_api = self.hid_api.lock().unwrap();
    let new_device = hid_api.open(vendor_id, product_id).unwrap();
    let mut device_guard = self.device.lock().unwrap();
    *device_guard = Some(new_device);
    Ok(())
  }

  pub fn close(&self) -> crate::Result<()> {
    let mut device_guard = self.device.lock().unwrap();
    *device_guard = None;
    Ok(())
  }

  pub fn write(&self, data: &[u8]) -> crate::Result<()> {
    let device_guard = self.device.lock().unwrap();
    let device = device_guard.as_ref().unwrap();
    device.write(data).unwrap();
    Ok(())
  }

  pub fn read(&self, size: usize) -> crate::Result<Vec<u8>> {
    let device_guard = self.device.lock().unwrap();
    let device = device_guard.as_ref().unwrap();
    let mut buffer = vec![0; size];
    device.read(&mut buffer).unwrap();
    Ok(buffer)
  }
}
