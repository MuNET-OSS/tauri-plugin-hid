use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HidDeviceInfo {
  pub path: String,
  pub vendor_id: u16,
  pub product_id: u16,
  pub serial_number: Option<String>,
  pub release_number: u16,
  pub manufacturer_string: Option<String>,
  pub product_string: Option<String>,
}