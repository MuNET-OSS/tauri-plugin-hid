package uk.redfern.tauri.plugin.hid

import android.app.Activity
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.JSArray
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.content.Context

@InvokeArg
class PingArgs {
  var value: String? = null
}

@TauriPlugin
class ExamplePlugin(private val activity: Activity): Plugin(activity) {
    private val implementation = Example()

    @Command
    fun ping(invoke: Invoke) {
        val args = invoke.parseArgs(PingArgs::class.java)

        val ret = JSObject()
        ret.put("value", implementation.pong(args.value ?: "default value :("))
        invoke.resolve(ret)
    }

    @Command
    fun enumerate(invoke: Invoke) {
        val ret = JSObject()
        val devices = JSArray()

        val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList

        for (dev in deviceList.values) {
            val device = JSObject()
            device.put("path", dev.deviceName)
            device.put("vendorId", dev.vendorId)
            device.put("productId", dev.productId)
            device.put("serialNumber", dev.serialNumber)
            device.put("releaseNumber", 0)
            device.put("manufacturerString", dev.manufacturerName)
            device.put("productString", dev.productName)
            devices.put(device)
        }

        ret.put("devices", devices)
        invoke.resolve(ret)
    }
}
