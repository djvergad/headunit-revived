package com.andrerinas.headunitrevived.main

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.andrerinas.headunitrevived.App

import java.io.IOException
import java.net.InetAddress
import java.util.ArrayList
import java.util.HashSet
import java.util.Locale

import com.andrerinas.headunitrevived.R
import com.andrerinas.headunitrevived.aap.AapService
import com.andrerinas.headunitrevived.utils.Settings
import com.andrerinas.headunitrevived.utils.changeLastBit
import com.andrerinas.headunitrevived.utils.toInetAddress

/**
 * @author algavris
 * *
 * @date 05/11/2016.
 */

class NetworkListFragment : Fragment() {
    private lateinit var adapter: AddressAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        val recyclerView = view.findViewById<RecyclerView>(android.R.id.list)

        adapter = AddressAdapter(requireContext(), childFragmentManager)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        return view
    }

    override fun onResume() {
        super.onResume()

        try {
            val currentIp = App.provide(requireContext()).wifiManager.connectionInfo.ipAddress
            adapter.currentAddress = currentIp.toInetAddress().changeLastBit(1).hostAddress ?: ""
        } catch (ignored: Exception) {
            adapter.currentAddress = ""
        }

        adapter.loadAddresses()
    }

    fun addAddress(ip: InetAddress) {
        adapter.addNewAddress(ip)
    }

    private class DeviceViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val removeButton = itemView.findViewById<Button>(android.R.id.button1)
        internal val startButton = itemView.findViewById<Button>(android.R.id.button2)
    }

    private class AddressAdapter(
                private val context: Context,
                private val fragmentManager: FragmentManager
    ) : RecyclerView.Adapter<DeviceViewHolder>(), View.OnClickListener {

        private val addressList = ArrayList<String>()
        var currentAddress: String = ""
        private val settings: Settings = Settings(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.list_item_device, parent, false)
            val holder = DeviceViewHolder(view)

            holder.startButton.setOnClickListener(this)
            holder.removeButton.setOnClickListener(this)
            holder.removeButton.setText(R.string.remove)
            return holder
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val ipAddress = addressList[position]

            val line1: String
            if (position == 0) {
                line1 = "Add a new address"
                holder.removeButton.visibility = View.GONE
            } else {
                line1 = ipAddress
                holder.removeButton.visibility = when (position) {
                    1 -> View.GONE
                    2 -> if (currentAddress.isNotEmpty()) View.GONE else View.VISIBLE
                    else -> View.VISIBLE
                }
            }
            holder.startButton.setTag(R.integer.key_position, position)
            holder.startButton.text = line1
            holder.startButton.setTag(R.integer.key_data, ipAddress)
            holder.removeButton.setTag(R.integer.key_data, ipAddress)
        }

        override fun getItemCount(): Int {
           return addressList.size
        }

        override fun onClick(v: View) {
            if (v.id == android.R.id.button2) {
                if (v.getTag(R.integer.key_position) == 0) {
                    var ip: InetAddress? = null
                    try {
                        val ipInt = App.provide(context).wifiManager.connectionInfo.ipAddress
                        ip = ipInt.toInetAddress()
                    } catch (ignored: IOException) {
                    }

                    AddNetworkAddressDialog.show(ip, fragmentManager)
                } else {
                    context.startService(AapService.createIntent(v.getTag(R.integer.key_data) as String, context))
                }
            } else {
                this.removeAddress(v.getTag(R.integer.key_data) as String)
            }
        }

        internal fun addNewAddress(ip: InetAddress) {
            val newAddrs = HashSet(settings.networkAddresses)
            newAddrs.add(ip.hostAddress)
            settings.networkAddresses = newAddrs
            set(newAddrs)
        }

        internal fun loadAddresses() {
            set(settings.networkAddresses)
        }

        private fun set(addrs: Collection<String>) {
            addressList.clear()
            addressList.add("")
            addressList.add("127.0.0.1")
            if (currentAddress.isNotEmpty()) {
                addressList.add(currentAddress)
            }
            addressList.addAll(addrs)
            notifyDataSetChanged()
        }

        private fun removeAddress(ipAddress: String) {
            val newAddrs = HashSet(settings.networkAddresses)
            newAddrs.remove(ipAddress)
            settings.networkAddresses = newAddrs
            set(newAddrs)
        }

    }

    companion object {
        const val TAG = "NetworkListFragment"
    }
}
