use tauri::{AppHandle, command, Runtime};

use crate::models::*;
use crate::Result;
use crate::HidExt;

#[command]
pub(crate) async fn ping<R: Runtime>(
    app: AppHandle<R>,
    payload: PingRequest,
) -> Result<PingResponse> {
    app.hid().ping(payload)
}
