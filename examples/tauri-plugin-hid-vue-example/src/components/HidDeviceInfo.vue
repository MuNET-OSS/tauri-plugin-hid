<script lang="ts" setup>
import { DeviceInfo } from "@redfernelec/tauri-plugin-hid-api";

defineProps<{
  device: DeviceInfo;
}>();
</script>

<template>
  <div class="device-info">
    <h1>{{ device.productString }} ({{ device.manufacturerString }})</h1>
    <p>Vendor ID: {{ device.vendorId }}</p>
    <p>Product ID: {{ device.productId }}</p>
    <p>Serial Number: {{ device.serialNumber }}</p>
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