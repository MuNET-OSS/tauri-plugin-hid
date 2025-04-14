package uk.redfern.tauri.plugin.hid

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSArray
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin

private const val TAG = "HidPlugin"

sealed class HidResult<out T> {
    data class Success<T>(val data: T) : HidResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : HidResult<Nothing>() {
        init {
            Log.e(TAG, message, exception)
        }
    }
}

class HidDevice(
    private val usbManager: UsbManager,
    private val usbDevice: UsbDevice,
    private val deviceConnection: UsbDeviceConnection
) {
    private var usbInEndpoint: UsbEndpoint? = null
    private var usbOutEndpoint: UsbEndpoint? = null
    private var usbInterface: UsbInterface? = null
    
    // Initialize and connect to the device
    fun initialize(): HidResult<Unit> {
        try {
            val usbInterface = usbDevice.getInterface(0)
            this.usbInterface = usbInterface
            
            // Log device details
            Log.i(TAG, "Interface Count: ${usbDevice.interfaceCount}")
            Log.i(TAG, "Using Interface ID: ${usbInterface.id}")
            Log.i(TAG, "Interface Class: ${usbInterface.interfaceClass}")
            Log.i(TAG, "Interface Subclass: ${usbInterface.interfaceSubclass}")
            Log.i(TAG, "Interface Protocol: ${usbInterface.interfaceProtocol}")
            Log.i(TAG, "Interface Endpoint Count: ${usbInterface.endpointCount}")
            
            // Find IN and OUT endpoints
            for (i in 0 until usbInterface.endpointCount) {
                val endpoint: UsbEndpoint = usbInterface.getEndpoint(i)
                Log.i(TAG, "Endpoint ID: ${endpoint.endpointNumber}")
                Log.i(TAG, "Endpoint Type: ${endpoint.type}")
                Log.i(
                    TAG,
                    "Endpoint Direction: ${if (endpoint.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"}"
                )
                if (endpoint.direction == UsbConstants.USB_DIR_IN && endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    Log.i(TAG, "Found IN endpoint")
                    usbInEndpoint = endpoint
                }
                if (endpoint.direction == UsbConstants.USB_DIR_OUT && endpoint.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    Log.i(TAG, "Found OUT endpoint")
                    usbOutEndpoint = endpoint
                }
            }
            
            // Claim interface
            val claimed = deviceConnection.claimInterface(usbInterface, true)
            if (!claimed) {
                return HidResult.Error("Failed to claim interface")
            }
            deviceConnection.setInterface(usbInterface)
            Log.i(TAG, "Interface claimed")
            return HidResult.Success(Unit)
        } catch (e: Exception) {
            return HidResult.Error("Error initializing device: ${e.message}", e)
        }
    }

    // Read data from the device
    fun read(timeout: Int): HidResult<ByteArray> {
        val endpoint = usbInEndpoint
        
        if (endpoint == null) {
            return HidResult.Error("Cannot read: IN endpoint not available")
        }
        
        val buffer = ByteArray(endpoint.maxPacketSize)
        val bytesRead = deviceConnection.bulkTransfer(endpoint, buffer, buffer.size, timeout)
        
        // Return the buffer truncated to the number of bytes read (bytesRead can be -1 on timeout)
        if (bytesRead <= 0) {
            return HidResult.Success(ByteArray(0))
        }
        return HidResult.Success(buffer.copyOf(bytesRead))
    }
    
    // Write data to the device
    fun write(data: ByteArray): HidResult<Unit> {
        val endpoint = usbOutEndpoint
        
        if (endpoint == null) {
            return HidResult.Error("Cannot write: OUT endpoint not available")
        }
        
        val bytesWritten = deviceConnection.bulkTransfer(endpoint, data, data.size, 1000)
        return if (bytesWritten > 0) {
            HidResult.Success(Unit)
        } else {
            HidResult.Error("Failed to write data")
        }
    }

    // Close the connection
    fun closeConnection(): HidResult<Unit> {
        if (usbInterface == null) {
            return HidResult.Error("Cannot close: Interface not available")
        }
        if (deviceConnection == null) {
            return HidResult.Error("Cannot close: Connection not available")
        }
        try {
            usbInterface?.let { intf ->
                deviceConnection.releaseInterface(intf)
            }
            deviceConnection.close()
            return HidResult.Success(Unit)
        } catch (e: Exception) {
            return HidResult.Error("Error closing device: ${e.message}", e)
        }
    }
}

// Argument classes
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
    var timeout: Int = 0
}

@InvokeArg
class WriteArgs {
    var path: String? = null
    var data: ByteArray? = null
}

@TauriPlugin
class HidPlugin(private val activity: Activity): Plugin(activity) {
    companion object {
        private const val ACTION_USB_PERMISSION = "uk.redfern.tauri.plugin.hid.USB_PERMISSION"
    }
    
    private val usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private val connectedDevices = HashMap<String, HidDevice>()
    private var permissionReceiver: BroadcastReceiver? = null
    private var usbDetachReceiver: BroadcastReceiver? = null
    
    // Permission handling state
    private var pendingDevicePath: String? = null
    private var pendingUsbDevice: UsbDevice? = null
    private var pendingInvoke: Invoke? = null

    init {
        registerUsbPermissionReceiver()
        registerUsbDetachReceiver()
    }

    private fun registerUsbDetachReceiver() {
        usbDetachReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.apply {
                        if (connectedDevices.containsKey(device.deviceName)) {
                            connectedDevices[device.deviceName]!!.closeConnection()
                            connectedDevices.remove(device.deviceName)
                        }
                        Log.i(TAG, "Device detached: ${device.deviceName}")
                    }
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        // TODO: Handle attach in future - emit events to frontend
        // filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        activity.registerReceiver(usbDetachReceiver, filter)
    }

    private fun registerUsbPermissionReceiver() {
        permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this@HidPlugin) {
                        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

                        Log.i(TAG, "Permission ${if (granted) "granted" else "denied"} for device ${device?.deviceName}")
                        
                        if (granted && device != null && pendingUsbDevice?.deviceName == device.deviceName) {
                            createAndConnectDevice(pendingUsbDevice!!, pendingDevicePath!!, pendingInvoke!!)
                        } else {
                            pendingInvoke?.reject("Permission denied for USB device")
                        }
                        
                        // Clear pending state
                        pendingInvoke = null
                        pendingUsbDevice = null
                        pendingDevicePath = null
                    }
                }
            }
        }
        
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        
        // Register with RECEIVER_EXPORTED flag on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            activity.registerReceiver(permissionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            activity.registerReceiver(permissionReceiver, filter)
        }
    }
    
    private fun requestPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            activity, 0, 
            Intent(ACTION_USB_PERMISSION),
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_IMMUTABLE
            }
        )
        usbManager.requestPermission(device, permissionIntent)
        Log.i(TAG, "Permission requested for device: ${device.deviceName}")
    }
    
    private fun createAndConnectDevice(usbDevice: UsbDevice, path: String, invoke: Invoke) {
        // Open device connection
        val connection = usbManager.openDevice(usbDevice)
        if (connection == null) {
            Log.e(TAG, "Failed to open connection to device: ${usbDevice.deviceName}")
            invoke.reject("Failed to open connection to device")
            return
        }
        
        // Create new HidDevice
        val hidDevice = HidDevice(usbManager, usbDevice, connection)

        when (val result = hidDevice.initialize()) {
            is HidResult.Success -> {
                Log.i(TAG, "HidDevice created successfully")
                connectedDevices[path] = hidDevice
                invoke.resolve()
            }
            is HidResult.Error -> {
                invoke.reject(TAG, "Failed to create HidDevice: ${result.message}")
            }
            else -> {
                invoke.reject("Unknown error")
            }
        }
    }

    // TODO: Call this when the plugin is destroyed
    // or when the app is closed
    // @Command ? 
    fun cleanup() {
        if (permissionReceiver != null) {
            activity.unregisterReceiver(permissionReceiver)
            permissionReceiver = null
        }

        // Close all open devices
        for (device in connectedDevices.values) {
            device.closeConnection()
        }
        connectedDevices.clear()
    }

    @Command
    fun enumerate(invoke: Invoke) {
        val ret = JSObject()
        val devices = JSArray()

        val deviceList = usbManager.deviceList

        for (dev in deviceList.values) {
            if(!connectedDevices.containsKey(dev.deviceName)) {
                val device = JSObject()
                device.put("releaseNumber", 0)
                device.put("path", dev.deviceName)
                device.put("vendorId", dev.vendorId)
                device.put("productId", dev.productId)
                // TODO: Can't read serial on Android unless we have permission (i.e. connected) - updated when connected and figure out how to pass to frontend
                // device.put("serialNumber", dev.serialNumber)
                device.put("manufacturerString", dev.manufacturerName)
                device.put("productString", dev.productName)
                devices.put(device)
            }
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
        val path = args.path!!

        val deviceList = usbManager.deviceList

        if (deviceList.containsKey(path)) {
            val device = deviceList[path]
            if (device != null) {
                Log.i(TAG, "Device found: ${device.deviceName}")
                
                // Check if we already have this device open
                val existingDevice = connectedDevices[path]
                if (existingDevice != null) {
                    invoke.resolve()
                    return
                }
                
                // Check for permission
                if (usbManager.hasPermission(device)) {
                    // We have permission, create and connect
                    createAndConnectDevice(device, path, invoke)
                } else {
                    // No permission, request it first
                    synchronized(this) {
                        pendingDevicePath = path
                        pendingUsbDevice = device
                        pendingInvoke = invoke
                    }
                    requestPermission(device)
                }
                return
            } else {
                invoke.reject("Device not found")
            }
        } else {
            invoke.reject("Path not found in device list")
        }
    }
    
    @Command
    fun close(invoke: Invoke) {
        val args = invoke.parseArgs(CloseArgs::class.java)
        if (args.path == null) {
            invoke.reject("Path is required")
            return
        }
        
        val path = args.path!!
        val device = connectedDevices[path]
        if (device != null) {
            when (device.closeConnection()) {
                is HidResult.Error -> {
                    invoke.reject("Failed to close device")
                    return
                }
                is HidResult.Success -> {
                    Log.i(TAG, "Device closed: $path")
                }
                else -> {
                    invoke.reject("Unknown error")
                    return
                }
            }
            connectedDevices.remove(path)
            invoke.resolve()
        } else {
            invoke.reject("Device not open")
        }
    }
    
    @Command
    fun read(invoke: Invoke) {
        val args = invoke.parseArgs(ReadArgs::class.java)
        if (args.path == null) {
            invoke.reject("Path is required")
            return
        }
        
        val path = args.path!!
        val device = connectedDevices[path]
        if (device == null) {
            invoke.reject("Device not open")
            return
        }
        
        when (val result = device.read(args.timeout)) {
            is HidResult.Success -> {
                val ret = JSObject()
                ret.put("data", JSArray(result.data))
                invoke.resolve(ret)
            }
            is HidResult.Error -> {
                invoke.reject("Failed to read from device: ${result.message}")
            }
            else -> {
                invoke.reject("Unknown error")
            }
        }
    }
    
    @Command
    fun write(invoke: Invoke) {
        val args = invoke.parseArgs(WriteArgs::class.java)
        if (args.path == null || args.data == null) {
            invoke.reject("Path and data are required")
            return
        }
        
        val path = args.path!!
        val device = connectedDevices[path]
        if (device == null) {
            invoke.reject("Device not open")
            return
        }
        
        when (val result = device.write(args.data!!)) {
            is HidResult.Success -> {
                invoke.resolve()
            }
            is HidResult.Error -> {
                invoke.reject("Failed to write to device: ${result.message}")
            }
            else -> {
                invoke.reject("Unknown error")
            }
        }
    }
}
