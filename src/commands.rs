use tauri::{AppHandle, command, Runtime};
use uuid::Uuid;

use crate::Result;
use crate::HidExt;

#[command]
pub(crate) async fn enumerate<R: Runtime>(
    app: AppHandle<R>,
) -> Result<Vec<crate::HidDeviceInfo>> {
    app.hid().enumerate()
}

#[command]
pub(crate) async fn open<R: Runtime>(
    app: AppHandle<R>,
    vendor_id: u16,
    product_id: u16,
) -> Result<String> {
    Ok(app.hid().open(vendor_id, product_id)?.to_string())
}

#[command]
pub(crate) async fn close<R: Runtime>(
    app: AppHandle<R>,
    id: String,
) -> Result<()> {
    // Convert the string to a UUID
    let id = match Uuid::parse_str(&id) {
        Ok(id) => id,
        Err(_) => return Err(crate::Error::HidDeviceUuidInvalidFormat),
    };
    // Call the close method
    app.hid().close(id)
}

#[command]
pub(crate) async fn write<R: Runtime>(
    app: AppHandle<R>,
    id: String,
    data: Vec<u8>,
) -> Result<()> {
    // Convert the string to a UUID
    let id = match Uuid::parse_str(&id) {
        Ok(id) => id,
        Err(_) => return Err(crate::Error::HidDeviceUuidInvalidFormat),
    };
    app.hid().write(id, data.as_slice())
}

#[command]
pub(crate) async fn read<R: Runtime>(
    app: AppHandle<R>,
    id: String,
    timeout: i32
) -> Result<Vec<u8>> {
    // Convert the string to a UUID
    let id = match Uuid::parse_str(&id) {
        Ok(id) => id,
        Err(_) => return Err(crate::Error::HidDeviceUuidInvalidFormat),
    };
    app.hid().read(id, timeout)
}