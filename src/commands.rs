use tauri::{AppHandle, command, Runtime};

use crate::Result;
use crate::HidExt;

#[command]
pub(crate) async fn device_list<R: Runtime>(
    app: AppHandle<R>,
) -> Result<Vec<crate::DeviceInfo>> {
    app.hid().device_list()
}

#[command]
pub(crate) async fn open<R: Runtime>(
    app: AppHandle<R>,
    vendor_id: u16,
    product_id: u16,
) -> Result<()> {
    app.hid().open(vendor_id, product_id)
}

#[command]
pub(crate) async fn close<R: Runtime>(
    app: AppHandle<R>,
) -> Result<()> {
    app.hid().close()
}

#[command]
pub(crate) async fn write<R: Runtime>(
    app: AppHandle<R>,
    data: Vec<u8>,
) -> Result<()> {
    app.hid().write(data.as_slice())
}

#[command]
pub(crate) async fn read<R: Runtime>(
    app: AppHandle<R>,
    size: usize,
) -> Result<Vec<u8>> {
    app.hid().read(size)
}