package com.example.mybluetoothchattest

import android.bluetooth.BluetoothDevice

data class DeviceData(val deviceName: String, val deviceMACAddress: String, val device: BluetoothDevice)