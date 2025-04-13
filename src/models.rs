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

#[derive(Debug, Clone, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct EnumerateResponse {
  pub devices: Vec<HidDeviceInfo>,
}

#[derive(Debug, Clone, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OpenRequest {
  pub path: String,
}

#[derive(Debug, Clone, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReadRequest {
  pub path: String,
  pub timeout: i32,
}

#[derive(Debug, Clone, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ReadResponse {
  pub data: Vec<u8>,
}

#[derive(Debug, Clone, Default, Deserialize, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WriteRequest {
  pub path: String,
  pub data: Vec<u8>,
}