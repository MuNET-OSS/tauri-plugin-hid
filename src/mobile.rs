use serde::de::DeserializeOwned;
use tauri::{
    plugin::{PluginApi, PluginHandle},
    AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_hid);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
    _app: &AppHandle<R>,
    api: PluginApi<R, C>,
) -> crate::Result<Hid<R>> {
    #[cfg(target_os = "android")]
    let handle = api.register_android_plugin("uk.redfern.tauri.plugin.hid", "ExamplePlugin")?;
    #[cfg(target_os = "ios")]
    let handle = api.register_ios_plugin(init_plugin_hid)?;
    Ok(Hid(handle))
}

/// Access to the hid APIs.
pub struct Hid<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> Hid<R> {
    // pub fn ping(&self, payload: PingRequest) -> crate::Result<PingResponse> {
    //   self
    //     .0
    //     .run_mobile_plugin("ping", payload)
    //     .map_err(Into::into)
    // }

    pub fn enumerate(&self) -> crate::Result<Vec<HidDeviceInfo>> {
        let response: EnumerateResponse = self
            .0
            .run_mobile_plugin("enumerate", ())
            .map_err(Into::into)?;
        Ok(response.devices)
    }

    pub fn open(&self, path: &str) -> crate::Result<()> {
        Err(crate::Error::HidDeviceNotFound)
    }

    pub fn close(&self, path: &str) -> crate::Result<()> {
        Err(crate::Error::HidDeviceNotFound)
    }

    pub fn write(&self, path: &str, data: &[u8]) -> crate::Result<()> {
        Err(crate::Error::HidDeviceNotFound)
    }

    pub fn read(&self, path: &str, timeout: i32) -> crate::Result<Vec<u8>> {
        Err(crate::Error::HidDeviceNotFound)
    }
}
