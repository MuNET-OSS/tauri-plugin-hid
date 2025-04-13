package uk.redfern.tauri.plugin.hid

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.util.Log
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.JSObject
import app.tauri.plugin.JSArray
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke

class HidDevice(
    val activity: Activity,
    val usbDevice: UsbDevice,
    val usbManager: UsbManager
) {
    var usbInEndpoint: UsbEndpoint? = null
    var usbOutEndpoint: UsbEndpoint? = null
    var usbDeviceConnection: UsbDeviceConnection? = null

    init {
        val usbInterface: UsbInterface = usbDevice.getInterface(0)
        Log.i("USB", "Interface Count: ${usbDevice.interfaceCount}")
        Log.i("USB", "Interface ID: ${usbInterface.id}")
        Log.i("USB", "Interface Class: ${usbInterface.interfaceClass}")
        Log.i("USB", "Interface Subclass: ${usbInterface.interfaceSubclass}")
        Log.i("USB", "Interface Protocol: ${usbInterface.interfaceProtocol}")
        Log.i("USB", "Interface Name: ${usbInterface.name}")
        Log.i("USB", "Interface Endpoint Count: ${usbInterface.endpointCount}")

        for (i in 0 until usbInterface.endpointCount) {
            val endpoint: UsbEndpoint = usbInterface.getEndpoint(i)
            Log.i("USB", "Endpoint ID: ${endpoint.endpointNumber}")
            Log.i("USB", "Endpoint Type: ${endpoint.type}")
            Log.i(
                "USB",
                "Endpoint Direction: ${if (endpoint.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"}"
            )
            if (endpoint.direction == UsbConstants.USB_DIR_IN && endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                Log.i("USB", "Found IN endpoint")
                usbInEndpoint = endpoint;
            }
            if (endpoint.direction == UsbConstants.USB_DIR_OUT && endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                Log.i("USB", "Found OUT endpoint")
                usbOutEndpoint = endpoint;
            }
        }

        if (usbManager.hasPermission(usbDevice)) {
                Log.i("USB", "Permission granted")
        } else {
            Log.i("USB", "Permission not granted")
            val permissionIntent =
                PendingIntent.getBroadcast(activity,0, Intent("ACTION_USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE)

            usbManager.requestPermission(usbDevice, permissionIntent)
            Log.i("USB", "Permission requested")
//            return
        }

        usbDeviceConnection = usbManager.openDevice(usbDevice);
        Log.i("USB", "Connection opened");

        usbDeviceConnection?.claimInterface(usbInterface, true)
        usbDeviceConnection?.setInterface(usbInterface);
        Log.i("USB", "Interface claimed");
    }

    fun closeConnection() {
        usbDeviceConnection?.close()
    }
}

@InvokeArg
class PingArgs {
    var value: String? = null
}

@InvokeArg
class OpenArgs {
    var path: String? = null
}

@InvokeArg
class CloseArgs {
    var path: String? = null
}

@InvokeArg
class ReadArgs {
    var path: String? = null
    var timeout: Int = 0;
}

@InvokeArg
class WriteArgs {
    var path: String? = null
    var data: ByteArray? = null
}

@TauriPlugin
class ExamplePlugin(private val activity: Activity): Plugin(activity) {
    private val implementation = Example()
    val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager

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
            device.put("releaseNumber", 0)
             device.put("path", dev.deviceName)
             device.put("vendorId", dev.vendorId)
             device.put("productId", dev.productId)
//             device.put("serialNumber", dev.serialNumber)
             device.put("manufacturerString", dev.manufacturerName)
             device.put("productString", dev.productName)
            devices.put(device)
        }

        ret.put("devices", devices)
        invoke.resolve(ret)
    }

    @Command
    fun open(invoke: Invoke) {
        val args = invoke.parseArgs(OpenArgs::class.java)
                if (args.path == null) {
            invoke.reject("Path is required")
            return
        }
        val path = args.path

        val deviceList = usbManager.deviceList

        if (deviceList.containsKey(path)) {
            val device = deviceList[path]
            if (device != null) {
//                val hidDevice = HidDevice(device)
                Log.i("USB", "Device found: ${device.deviceName}")
                // val ret = JSObject()
                // ret.put("device", hidDevice)
                // invoke.resolve(ret)
            } else {
                invoke.reject("Device not found")
            }
        } else {
            invoke.reject("Path not found in device list")
        }

        invoke.resolve()
    }
}
