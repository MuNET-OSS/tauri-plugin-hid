<script lang="ts" setup>
import { HidDevice } from "@redfernelec/tauri-plugin-hid-api";

defineProps<{
  device: HidDevice;
}>();
</script>

<template>
  <div class="device-info">
    <h1>{{ device.productString }} ({{ device.manufacturerString }})</h1>
    <p>Vendor ID: 0x{{ device.vendorId.toString(16).toUpperCase().padStart(4, '0') }}</p>
    <p>Product ID: 0x{{ device.productId.toString(16).toUpperCase().padStart(4, '0') }}</p>
    <p>Serial Number: {{ device.serialNumber }}</p>
    <p>Path: {{  device.path }}</p>
  </div>
</template>

<style scoped>
  .device-info {
    border: 1px solid #ccc;
    padding: 10px;
    margin: 10px;
    border-radius: 5px;
    border-color: #ccc;
  }

  .device-info:hover {
    border-color: #396cd8;
  }
</style>