package com.example.mybluetoothchattest

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(){

    private val btBroadcastReceiver = BtBroadcastReceiver()
    var findDeviceList: ArrayList<DeviceData> = ArrayList()
    private lateinit var foundDeviceListView: ListView
    private lateinit var pairedDeviceList:List<DeviceData>
    private lateinit var pairedDeviceListView: ListView
    private lateinit var statusLayout: LinearLayout
    private lateinit var textView1: TextView
    private lateinit var textView2: TextView

    private var messageList = ArrayList<String>()
    private lateinit var messageListView: ListView
    private lateinit var sendButtonLayout: LinearLayout
    private lateinit var sendEditText: EditText


    lateinit var bluetoothAdapter: BluetoothAdapter

    lateinit var bluetoothService: BluetoothChatSocket

    lateinit var handler: BluetoothMsgHandler

    //private val mHandler = MyHandler()

    //@RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionCheck()

        //messageViewMode()

        foundDeviceListView = findViewById(R.id.deviceView)
        foundDeviceListView.adapter  =DeviceListAdapter(applicationContext, R.layout.device_list_layout, findDeviceList)

        //ブロードキャストレシーバーを登録している
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        registerReceiver(btBroadcastReceiver, intentFilter)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        findViewById<Button>(R.id.makeVisibleButton).setOnClickListener{
            discoverableDevice()
        }

        findViewById<Button>(R.id.searchDeviceButton).setOnClickListener{
            searchDevice()
        }

        //ペアリング済みのデバイスを表示する処理
        pairedDeviceList = bluetoothAdapter.bondedDevices.map{
            DeviceData(it.name, it.address, it)
        }
        statusLayout = findViewById(R.id.statusLayout)
        textView1 = findViewById(R.id.textView)
        textView2 = findViewById(R.id.textView2)
        pairedDeviceListView = findViewById(R.id.pairDeviceList)
        pairedDeviceListView.adapter = DeviceListAdapter(applicationContext, R.layout.device_list_layout, pairedDeviceList)
        pairedDeviceListView.setOnItemClickListener { _, _, position, _ ->
            Log.d("CONNECT", "デバイスに接続しようとしています")
            connectDevice(pairedDeviceList[position])
        }

        foundDeviceListView.setOnItemClickListener { _, _, position, _ ->
            connectDevice(findDeviceList[position])
            Log.d("CONNECT", "デバイスに接続しようとしています")
        }


        messageListView = findViewById(R.id.messageView)
        sendButtonLayout = findViewById(R.id.sendButtonLayout)
        sendEditText = findViewById(R.id.sendText)
        handler = BluetoothMsgHandler()
        bluetoothService = BluetoothChatSocket(bluetoothAdapter,handler, applicationContext){
            //Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
            messageList.add(it)
            val arrayAdapter: ArrayAdapter<String> =  ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, messageList)
            messageListView.adapter = arrayAdapter
        }

        sendButton.setOnClickListener{
            val text = sendEditText.text.toString()
            bluetoothService.send(text)
            sendEditText.editableText.clear()
        }


    }

    override fun onStart(){
        super.onStart()
        selectViewMode()
    }

    override fun onResume(){
        super.onResume()
            bluetoothService.acceptSocket(true){
                messageViewMode()
            }
    }

    private fun connectDevice(deviceData: DeviceData){

        //Bluetoothデバイスの捜索をキャンセルする
        bluetoothAdapter.cancelDiscovery()

        val device = bluetoothAdapter.getRemoteDevice(deviceData.device.address)

        messageViewMode()
        bluetoothService.connectSocket(device, true)
        bluetoothService.acceptSocketJob?.cancel()

    }

    private fun setAdapter(list: ArrayList<DeviceData> = findDeviceList){
        foundDeviceListView.adapter = DeviceListAdapter(applicationContext, R.layout.device_list_layout, list)
    }

    private fun discoverableDevice(){
        //自デバイスを検出可能にするための検出許可画面を呼び出すためのIntent
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)

        //120秒間発見可能にする設定
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        startActivity(discoverableIntent)


    }

    private fun messageViewMode(){
        foundDeviceListView.visibility = View.GONE
        pairedDeviceListView.visibility = View.GONE
        statusLayout.visibility = View.GONE
        textView1.visibility = View.GONE
        textView2.visibility = View.GONE

        messageListView.visibility = View.VISIBLE
        sendButtonLayout.visibility = View.VISIBLE

    }

    private fun selectViewMode(){
        foundDeviceListView.visibility = View.VISIBLE
        pairedDeviceListView.visibility = View.VISIBLE
        statusLayout.visibility = View.VISIBLE
        textView2.visibility = View.VISIBLE
        textView1.visibility = View.VISIBLE

        messageListView.visibility = View.GONE
        sendButtonLayout.visibility = View.GONE
    }


    @SuppressLint("ShowToast")
    private fun permissionCheck(){
        Log.d("Version", Build.VERSION.SDK_INT.toString())
        //Lollipop以前のAndroidはBluetoothLEの関係上強制終了する

        //Android M以下のデバイスはcheckSelfPermissionを使用することができないので、ここで終了する
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){

            Toast.makeText(applicationContext, "Android5.0未満のデバイスは対応していません。", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            AlertDialog.Builder(this).apply{
                setTitle("位置情報の提供を許可しますか？")
                setMessage("このアプリケーションは位置情報の許可を必要としています。")
                setPositiveButton(android.R.string.ok, null)
                setOnDismissListener{
                    requestPermissions(arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),123)
                }.show()

            }
        }else{

        }
    }

    private fun searchDevice(){
        if(bluetoothAdapter.isDiscovering){
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(btBroadcastReceiver)
    }

    inner class BtBroadcastReceiver : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {

            if(intent == null || intent.action != BluetoothDevice.ACTION_FOUND){
                return
            }

            val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val deviceName = device.name
            val deviceMACAddress = device.address

            Log.d("Device","発見したデバイス$deviceName,$deviceMACAddress")
            val data = DeviceData(deviceName, deviceMACAddress, device)
            findDeviceList.remove(data)
            findDeviceList.add(data)

            setAdapter()


        }
    }

    /*inner class MyHandler(): Handler(){
        override fun handleMessage(msg: Message?) {

        }
    }*/

    @SuppressLint("HandlerLeak")
    inner class BluetoothMsgHandler(): Handler(){
        override fun handleMessage(msg: Message?) {

            Log.d("Handler", "Handlerが呼び出された")

            when(msg?.what){
                1 ->{
                    Toast.makeText(applicationContext, msg as String, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
