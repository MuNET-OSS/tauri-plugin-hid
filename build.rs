const COMMANDS: &[&str] = &["enumerate", "open", "close", "write", "read", "send_output_report", "get_input_report"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}
