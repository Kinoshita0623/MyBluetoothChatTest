package com.example.mybluetoothchattest

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView

class DeviceListAdapter(private val context: Context,itemLayoutId:Int, private val deviceList: List<DeviceData>): BaseAdapter(){

    data class ViewHolder(val deviceName:TextView, val deviceMACAddress: TextView)

    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private val layoutId = itemLayoutId


    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getItem(position: Int): Any {
        return deviceList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertViewTmp: View? = convertView

        val holder: ViewHolder

        if(convertViewTmp == null){
            convertViewTmp = inflater.inflate(layoutId, null)
            holder = ViewHolder(convertViewTmp!!.findViewById(R.id.deviceName), convertViewTmp.findViewById(R.id.macAddress))
            convertViewTmp.tag = holder
        }else{
            holder = convertViewTmp.tag as ViewHolder
        }

        holder.deviceName.text = deviceList[position].deviceName
        holder.deviceMACAddress.text = deviceList[position].deviceMACAddress

        return convertViewTmp

    }

}