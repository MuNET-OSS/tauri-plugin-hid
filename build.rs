const COMMANDS: &[&str] = &["device_list", "open", "close", "write", "read"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}
