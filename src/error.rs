use serde::{ser::Serializer, Serialize};
pub type Result<T> = std::result::Result<T, Error>;

#[derive(Debug, thiserror::Error)]
pub enum Error {
  #[error(transparent)]
  Io(#[from] std::io::Error),
  #[error("HidApi Error: {0}")]
  HidApiError(#[from] hidapi_rusb::HidError),
  #[error("Device not found")]
  HidDeviceNotFound,
  #[error("Device uuid no longer exists")]
  HidDeviceUuidNotFound,
  #[error("Invalid uuid format")]
  HidDeviceUuidInvalidFormat,
  #[error("HID read timed out")]
  HidReadTimeout,
  #[cfg(mobile)]
  #[error(transparent)]
  PluginInvoke(#[from] tauri::plugin::mobile::PluginInvokeError),
}

impl Serialize for Error {
  fn serialize<S>(&self, serializer: S) -> std::result::Result<S::Ok, S::Error>
  where
    S: Serializer,
  {
    serializer.serialize_str(self.to_string().as_ref())
  }
}
