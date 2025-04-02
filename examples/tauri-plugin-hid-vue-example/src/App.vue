<script setup lang="ts">
import { ref } from "vue";
import HidDeviceInfo from "./components/HidDeviceInfo.vue";
import { HidDevice, enumerate } from "@redfernelec/tauri-plugin-hid-api";

const devices = ref<HidDevice[]>([]);

async function test() {
  let logger: HidDevice | null = null;
  devices.value = await enumerate();
  
  let test_devs = await enumerate();
  console.log("Found devices:", test_devs);
  for (const device of test_devs) {
    if (device.productString === "Rocket Logger") {
      logger = device;
      break;
    }
  }

  if(logger) {
    console.log("Found logger device:", logger);
  }
  // if(logger) {
  //   console.log("Found logger device:", logger);
  //   await logger.open();
    
  //   console.log("Logger device opened", logger);
  //   await logger.write(new Uint8Array([0x00, 0x00]));
  //   let data = await logger.read(2);
  //   console.log("Logger device data:", data);

  //   console.log("Logger device opened", logger);
  //   await logger.write(new Uint8Array([0x00, 0x00]));
  //   data = await logger.read(2);
  //   console.log("Logger device data:", data);
    
  //   await logger.close();
  //   console.log("Logger device closed");
  // } else {
  //   console.log("Logger device not found");
  // }

  let logger2: HidDevice = new HidDevice();
  logger2.vendorId = 0x04D1;
  logger2.productId = 0xE5A3;
  try {
    await logger2.open();
    console.log("Logger2 device opened", logger2);

    await logger2.write(new Uint8Array([0x00, 0x00]));
    let data2 = await logger2.read(2);
    console.log("Logger2 device data:", data2);

    await logger2.close();
    console.log("Logger2 device closed");
  } catch (e) {
    console.error("Error opening logger2 device:", e);
  }
}
</script>

<template>
  <main class="container">
    <button v-on:click="test">Get devices
      <text v-if="devices.length !== 0"> (Found {{ devices.length }} devices)</text>
    </button>
    <HidDeviceInfo v-for="device in devices" :device="device" />
  </main>
</template>

<style>
:root {
  font-family: Inter, Avenir, Helvetica, Arial, sans-serif;
  font-size: 16px;
  font-weight: 400;

  color: #0f0f0f;
  background-color: #f6f6f6;

  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  -webkit-text-size-adjust: 100%;
}

.container {
  margin: 0;
  /* padding-top: 10vh; */
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.row {
  display: flex;
  justify-content: center;
}

a {
  font-weight: 500;
  color: #646cff;
  text-decoration: inherit;
}

a:hover {
  color: #535bf2;
}

h1 {
  text-align: center;
}

input,
button {
  border-radius: 8px;
  border: 1px solid transparent;
  padding: 0.6em 1.2em;
  font-size: 1em;
  font-weight: 500;
  font-family: inherit;
  color: #0f0f0f;
  background-color: #ffffff;
  transition: border-color 0.25s;
  box-shadow: 0 2px 2px rgba(0, 0, 0, 0.2);
}

button {
  cursor: pointer;
}

button:hover {
  border-color: #396cd8;
}

button:active {
  border-color: #396cd8;
  background-color: #e8e8e8;
}

@media (prefers-color-scheme: dark) {
  :root {
    color: #f6f6f6;
    background-color: #2f2f2f;
  }

  a:hover {
    color: #24c8db;
  }

  input,
  button {
    color: #ffffff;
    background-color: #0f0f0f98;
  }

  button:active {
    background-color: #0f0f0f69;
  }
}
</style> -->