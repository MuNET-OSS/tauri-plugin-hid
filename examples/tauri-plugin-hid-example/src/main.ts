import { deviceList, open, close, read, write } from "tauri-plugin-hid-api";

let greetInputEl: HTMLInputElement | null;
let greetMsgEl: HTMLElement | null;

async function test () {
  // console.log(await ping("Hello from Tauri!"));
  console.log(await deviceList());
  await open(0x1C40, 0x05B8);
  await write ([0x04, 0x02]);
  console.log(await read(64));
  await close();
}

window.addEventListener("DOMContentLoaded", () => {
  greetInputEl = document.querySelector("#greet-input");
  greetMsgEl = document.querySelector("#greet-msg");
  document.querySelector("#greet-form")?.addEventListener("submit", (e) => {
    e.preventDefault();
    test();
  });
});
