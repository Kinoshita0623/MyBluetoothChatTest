package com.example.mybluetoothchattest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class BluetoothChatSocket(private val bluetoothAdapter: BluetoothAdapter, private val handler: Handler, private val context: Context, val ui:(msg: String)->Unit){

    private val secureUUID = UUID.fromString("29621b37-e817-485a-a258-52da5261421a")
    private val insecureUUID = UUID.fromString("d620cd2b-e0a4-435b-b02e-40324d57195b")

    var acceptSocketJob:Job? = null
        private set
    var connectSocketJob: Job? = null
        private set

    var outputStream: OutputStream? = null

    fun send(msg: String){
        if(outputStream != null){
            outputStream!!.write(msg.toByteArray())
        }
    }

    private fun socketHandler(socket: BluetoothSocket?){
        if(socket == null){
            return
        }

        val buffer = ByteArray(1024)

        val inputStream: InputStream? = socket.inputStream
        outputStream = socket.outputStream

        GlobalScope.launch{
            try{
                while(socket.isConnected && inputStream != null && outputStream != null){
                    val next = inputStream.read(buffer)
                    val bufferArray = buffer.filter{
                        it != 0.toByte()
                    }.toByteArray()
                    val msg = String(bufferArray, 0, next)
                    Log.d("MSG", msg)

                    handler.post{
                        //Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        ui(msg)
                    }

                    handler.obtainMessage(1,0,0, msg)

                }
            }catch(e: Exception){
                Log.e("BLE", "ERROR", e)
            }
        }
    }

    fun connectSocket(device: BluetoothDevice, secure: Boolean){
        val socket = if(secure){
            device.createRfcommSocketToServiceRecord(secureUUID)
        }else{
            device.createInsecureRfcommSocketToServiceRecord(insecureUUID)
        }

        connectSocketJob = GlobalScope.launch{
            socket.connect()
            socketHandler(socket)
        }
    }

    fun acceptSocket(secure: Boolean, sendModeUi: ()->Unit){
        val serverSocket = if(secure){
            bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothSecure",secureUUID)
        }else{
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BluetoothInsecure", insecureUUID)
        }

        acceptSocketJob = GlobalScope.launch{
            val socket = try{
                serverSocket.accept()
            }catch(e: Exception){
                Log.e("Accept", "Acceptでエラー発生", e)
                acceptSocketJob?.cancel()
                null
            }


            socketHandler(socket)
            handler.post{
                sendModeUi()
            }
        }


    }
}