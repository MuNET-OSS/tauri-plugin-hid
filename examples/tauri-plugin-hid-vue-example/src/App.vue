<script setup lang="ts">
import { ref } from "vue";
import HidDeviceInfo from "./components/HidDeviceInfo.vue";
import HidDeviceConnected from "./components/HidDeviceConnected.vue";
import { HidDevice, enumerate } from "@redfernelec/tauri-plugin-hid-api";

let choosingDevice = ref(false);
const devices = ref<HidDevice[]>([]);
const connectedDevices = ref<HidDevice[]>([]);

async function chooseDevice() {
  devices.value = await enumerate();
  choosingDevice.value = true;
}

async function connect(device: HidDevice) {
  await device.open();
  if (device.id) {  // TODO: impletment a way to check if the device is already connected and use instead
    connectedDevices.value.push(device);
  }
  choosingDevice.value = false;
}

async function disconnect(device: HidDevice) {
  const index = connectedDevices.value.indexOf(device);
  if (index > -1) {
    connectedDevices.value.splice(index, 1);
  }
  await device.close();
}

async function back() {
  choosingDevice.value = false;
}
</script>

<template>
  <main class="container">
    <div v-if="!choosingDevice">
      <button v-on:click="chooseDevice"> Connect to a HID device </button>
      <HidDeviceConnected v-for="device in connectedDevices" :device="device" @disconnect="disconnect(device)"></HidDeviceConnected>
    </div>
    <div v-else>
      <button v-on:click="back"> Back </button>
      <p v-if="devices.length !== 0"> Found {{ devices.length }} devices </p>
      <p v-else> No devices found </p>
      <HidDeviceInfo v-for="device in devices" :device="device" @click="connect(device)"/>
    </div>
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
</style>