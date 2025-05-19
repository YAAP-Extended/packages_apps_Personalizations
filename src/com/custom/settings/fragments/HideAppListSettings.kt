package com.custom.settings.fragments

import android.app.ActivityManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.os.UserHandle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.core.InstrumentedPreferenceFragment
import com.android.internal.util.yaap.HideAppListUtils
import android.widget.CheckBox
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.provider.Settings

class HideAppListSettings : SettingsPreferenceFragment() {

    companion object {
        private const val TAG = "HideAppListSettings"
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    private lateinit var activityManager: ActivityManager
    private lateinit var packageManager: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var packageList: List<PackageInfo>
    private lateinit var userManager: UserManager
    private lateinit var userInfos: List<UserHandle>

    private var searchText = ""
    private var customFilter: ((PackageInfo) -> Boolean)? = null
    private var comparator: ((PackageInfo, PackageInfo) -> Int)? = null
    private var hideAppListUtils: HideAppListUtils = HideAppListUtils()
    private var showSystem = false
    private var showOverlay = false
    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityManager = requireContext().getSystemService(ActivityManager::class.java)
        packageManager = requireContext().packageManager
        userManager = requireContext().getSystemService(UserManager::class.java)
        userInfos = userManager.userProfiles
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            view.findViewById<RecyclerView>(R.id.recycler_view)?.let { rv ->
                recyclerView = rv
                
                if (!::adapter.isInitialized) {
                    adapter = AppListAdapter()
                }
                
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = this@HideAppListSettings.adapter
                }

                if (isAdded) {
                    refreshList()
                }
            } ?: run {
                Log.e(TAG, "Failed to find RecyclerView")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
        }
    }

    private fun refreshList() {
        if (!isAdded) return
        
        packageList = packageManager.getInstalledPackages(0)
            .filter { packageInfo ->
                val isSystemApp = (packageInfo.applicationInfo?.flags ?: 0 and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isOverlay = packageInfo.packageName.startsWith("com.android.systemui")
                
                when {
                    !showSystem && isSystemApp -> false
                    !showOverlay && isOverlay -> false
                    searchText.isNotEmpty() -> {
                        val label = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
                        label.contains(searchText, ignoreCase = true) ||
                        packageInfo.packageName.contains(searchText, ignoreCase = true)
                    }
                    customFilter != null -> customFilter!!(packageInfo)
                    else -> true
                }
            }
            .sortedWith { a, b ->
                val labelA = a.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
                val labelB = b.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
                labelA.compareTo(labelB)
            }
        
        adapter.notifyDataSetChanged()
    }

    private fun getInitialCheckedList(): List<String> {
        if (!isAdded) return emptyList()
        val flattenedString = Settings.Secure.getString(requireContext().contentResolver, getKey())
        return flattenedString?.takeIf { it.isNotBlank() }?.split(",")?.toList() ?: emptyList()
    }

    private fun getKey(): String {
        return Settings.Secure.HIDE_APPLIST
    }

    private fun onListUpdate(packageName: String, isChecked: Boolean) {
        if (!isAdded) return
        for (user in userInfos) {
            val userId = user.identifier
            if (isChecked) {
                hideAppListUtils.addApp(requireContext(), packageName, userId)
            } else {
                hideAppListUtils.removeApp(requireContext(), packageName, userId)
            }
        }
    }

    private inner class AppListAdapter : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
        private val selectedIndices = mutableSetOf<Int>()
        private var initialList = getInitialCheckedList().toMutableList()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val label: TextView = view.findViewById(R.id.app_label)
            val packageName: TextView = view.findViewById(R.id.app_package)
            val checkBox: CheckBox = view.findViewById(R.id.app_checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.hide_applist_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val packageInfo = packageList[position]
            val label = packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
            
            holder.label.text = label
            holder.packageName.text = packageInfo.packageName
            
            holder.itemView.setOnClickListener {
                if (selectedIndices.contains(position)) {
                    selectedIndices.remove(position)
                    onListUpdate(packageInfo.packageName, false)
                } else {
                    selectedIndices.add(position)
                    onListUpdate(packageInfo.packageName, true)
                }
                notifyItemChanged(position)
            }
            
            if (initialList.contains(packageInfo.packageName)) {
                initialList.remove(packageInfo.packageName)
                selectedIndices.add(position)
            }
            
            holder.checkBox.isChecked = selectedIndices.contains(position)
        }

        override fun getItemCount() = packageList.size
    }
} 