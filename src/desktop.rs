use std::vec;
use std::sync::Mutex;
use std::collections::HashMap;

use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};
use hidapi_rusb::HidApi;
use uuid::Uuid;

use crate::error::Error;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<Hid<R>> {
  Ok(Hid {
    app: app.clone(),
    hid_api: Mutex::new(HidApi::new().expect("Could not create HidApi instance")),
    open_devices: Mutex::new(HashMap::new()),
  })
}

/// Access to the hid APIs.
pub struct Hid<R: Runtime> {
  #[allow(dead_code)]
  app: AppHandle<R>,
  hid_api: Mutex<HidApi>,
  open_devices: Mutex<HashMap<Uuid, hidapi_rusb::HidDevice>>,
}

impl<R: Runtime> Hid<R> {
  pub fn enumerate(&self) -> crate::Result<Vec<crate::HidDeviceInfo>> {
    let mut devices = vec![];
    // Lock the mutex to get access to the HidApi instance. Will panic if the mutex is poisoned.
    // This should not happen in normal operation.
    let mut hid_api = self.hid_api.lock().unwrap(); 
    // Refresh HidApi devices to get the latest list of devices.
    hid_api.refresh_devices()?;
    // Loop through the devices and create a HidDeviceInfo for each one.
    for device in hid_api.device_list() {
      let device_info = crate::HidDeviceInfo {
        product_id: device.product_id(),
        vendor_id: device.vendor_id(),
        path: device.path().to_string_lossy().to_string(),
        manufacturer_string: match device.manufacturer_string() {
          Some(manufacturer_string) => Some(manufacturer_string.to_owned()),
          None => Some("".to_string())
        },
        product_string: match device.product_string() {
          Some(product_string) => Some(product_string.to_owned()),
          None => Some("".to_string())
        },
        serial_number: match device.serial_number() {
          Some(serial_number) => Some(serial_number.to_owned()),
          None => Some("".to_string())
        },
        release_number: device.release_number(),
      };
      devices.push(device_info);
    };

    Ok(devices)
  }

  pub fn open(&self, vendor_id: u16, product_id: u16) -> crate::Result<Uuid> {
    // Get a lock on the hid_api mutex and open_devices to ensure thread safety.
    // This will panic if the mutex is poisoned, which should not happen in normal operation.
    let hid_api = self.hid_api.lock().unwrap();
    let mut open_devices = self.open_devices.lock().unwrap();
    // Attempt to open the device with the given vendor_id and product_id.
    let new_device = hid_api.open(vendor_id, product_id)?;
    // Generate a new UUID for the device to keep a link to the object in frontend.
    let id = Uuid::new_v4();
    // Add the new device to the open_devices HashMap with the UUID as the key.
    open_devices.insert(id, new_device);
    // TODO:  Update properties like manufacturer_string, product_string, serial_number, etc. in the frontend.
    //        Maybe we need to pass back a whole DeviceInfo structure and include uuid there?
    // Return the UUID so it can be used to reference the device from frontend.
    Ok(id)
  }

  pub fn close(&self, id: Uuid) -> crate::Result<()> {
    // Get a lock on the open_devices mutex to ensure thread safety.
    // This will panic if the mutex is poisoned, which should not happen in normal operation.
    let mut open_devices = self.open_devices.lock().unwrap();
    // Remove the device from the open_devices HashMap using the UUID as the key.
    // This will also close the device as the HidDevice is dropped.
    open_devices.remove(&id);
    Ok(())
  }

  pub fn write(&self, id: Uuid, data: &[u8]) -> crate::Result<()> {
    // Get a lock on the open_devices mutex to ensure thread safety.
    // This will panic if the mutex is poisoned, which should not happen in normal operation.
    let open_devices = self.open_devices.lock().unwrap();
    // Get device from the open_devices HashMap using the UUID as the key.
    // If the device is not found, return an error.
    let device = match open_devices.get(&id) {
      Some(device) => device,
      None => return Err(crate::Error::HidDeviceUuidNotFound),
    };
    // Write data to the device.
    // TODO: Consider closing the device if write fails (to avoid stale device in list).
    device.write(data)?;
    Ok(())
  }

  pub fn read(&self, id: Uuid, timeout: i32) -> crate::Result<Vec<u8>> {
    // Get a lock on the open_devices mutex to ensure thread safety.
    // This will panic if the mutex is poisoned, which should not happen in normal operation.
    let open_devices = self.open_devices.lock().unwrap();
    // Get device from the open_devices HashMap using the UUID as the key.
    // If the device is not found, return an error.
    let device = match open_devices.get(&id) {
      Some(device) => device,
      None => return Err(crate::Error::HidDeviceUuidNotFound),
    };
    let mut buffer = vec![0; 64];
    // Read data from the device into the buffer.
    // The read method will block until data is available or an error occurs.
    // TODO: Consider closing the device if read fails (to avoid stale device in list).
    let len = device.read_timeout(&mut buffer, timeout)?;
    buffer.truncate(len);
    if len == 0 && timeout > 0 {
      Err(Error::HidReadTimeout)
    } else {
      Ok(buffer)
    }
  }
}
