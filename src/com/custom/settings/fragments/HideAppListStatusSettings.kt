/*
 * SPDX-FileCopyrightText: 2018-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2021 AOSP-Krypton Project
 * SPDX-FileCopyrightText: 2022 Nameless-AOSP Project
 * SPDX-FileCopyrightText: 2022 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.custom.settings.fragments

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.android.internal.util.yaap.HideAppListUtils
import com.android.settings.R
import com.google.android.material.appbar.AppBarLayout
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.ui.AdaptiveSwitchPreference
import com.android.settings.core.InstrumentedPreferenceFragment
import android.os.UserHandle
import android.util.Log
import android.view.LayoutInflater

class HideAppListStatusSettings : SettingsPreferenceFragment() {

  companion object {
    private const val TAG = "HideAppListStatusSettings"
    
    private val itemCallback = object : DiffUtil.ItemCallback<AppInfo>() {
      override fun areItemsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
        oldInfo.packageName == newInfo.packageName

      override fun areContentsTheSame(oldInfo: AppInfo, newInfo: AppInfo) = oldInfo == newInfo
    }
  }

  override fun getMetricsCategory(): Int {
    return InstrumentedPreferenceFragment.METRICS_CATEGORY_UNKNOWN
  }

  private lateinit var activityManager: ActivityManager
  private lateinit var packageManager: PackageManager
  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: AppListAdapter
  private lateinit var packageList: List<PackageInfo>
  private lateinit var userManager: UserManager
  private lateinit var userInfos: List<UserHandle>

  private var appBarLayout: AppBarLayout? = null
  private var searchText = ""
  private var customFilter: ((PackageInfo) -> Boolean)? = null
  private var comparator: ((PackageInfo, PackageInfo) -> Int)? = null
  private var hideAppListUtils: HideAppListUtils = HideAppListUtils()
  private var showSystem = false
  private var showOverlay = false
  private var optionsMenu: Menu? = null

  override fun onStart() {
    super.onStart()
    updateOptionsMenu()
    val host = getActivity()
    if (host != null) {
      host.invalidateOptionsMenu()
    }
  }

  @SuppressLint("QueryPermissionsNeeded")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    requireActivity().setTitle(getTitle())
    appBarLayout = requireActivity().findViewById(R.id.app_bar)
    activityManager =
      requireContext().getSystemService(ActivityManager::class.java) as ActivityManager
    packageManager = requireContext().packageManager
    userManager = requireContext().getSystemService(UserManager::class.java)
    userInfos = userManager.userProfiles
    for (info in userInfos) {
      hideAppListUtils.setApps(requireContext(), info.identifier)
    }
  }

  private fun getTitle(): Int {
    return R.string.hide_applist_title
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
          adapter = this@HideAppListStatusSettings.adapter
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

  /** @return an initial list of packages that should appear as selected. */
  private fun getInitialCheckedList(): List<String> {
    if (!isAdded()) return emptyList()
    val flattenedString = Settings.Secure.getString(requireContext().contentResolver, getKey())
    return flattenedString?.takeIf { it.isNotBlank() }?.split(",")?.toList() ?: emptyList()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val activity = getActivity()
    if (activity == null) {
      return
    }
    optionsMenu = menu
    inflater.inflate(R.menu.hide_applist_menu, menu)

    menu.findItem(R.id.show_system).setVisible(showSystem)
    menu.findItem(R.id.hide_system).setVisible(!showSystem)
    menu.findItem(R.id.show_overlay).setVisible(showSystem && showOverlay)
    menu.findItem(R.id.hide_overlay).setVisible(showSystem && !showOverlay)

    val searchMenuItem = menu.findItem(R.id.search) as MenuItem
    searchMenuItem.setOnActionExpandListener(
      object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
          // To prevent a large space on tool bar.
          appBarLayout!!.setExpanded(false /*expanded*/, false /*animate*/)
          // To prevent user can expand the collapsing tool bar view.
          ViewCompat.setNestedScrollingEnabled(recyclerView, false)
          return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
          // We keep the collapsed status after user cancel the search function.
          appBarLayout!!.setExpanded(false /*expanded*/, false /*animate*/)
          ViewCompat.setNestedScrollingEnabled(recyclerView, true)
          return true
        }
      }
    )
    val searchView = searchMenuItem.actionView as SearchView
    searchView.queryHint = getString(R.string.search_apps)
    searchView.setOnQueryTextListener(
      object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String) = false

        override fun onQueryTextChange(newText: String): Boolean {
          searchText = newText
          refreshList()
          return true
        }
      }
    )

    updateOptionsMenu()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.getItemId()) {
      R.id.show_system,
      R.id.hide_system -> {
        showSystem = !showSystem
        if (!showSystem) {
          showOverlay = false
        }
        refreshList()
      }
      R.id.show_overlay,
      R.id.hide_overlay -> {
        showOverlay = !showOverlay
        refreshList()
      }
    }
    updateOptionsMenu()
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    updateOptionsMenu()
  }

  override fun onDestroyOptionsMenu() {
    optionsMenu = null
  }

  private fun updateOptionsMenu() {
    if (optionsMenu == null) {
      return
    }

    var menu = optionsMenu as Menu

    menu.findItem(R.id.show_system).setVisible(!showSystem)
    menu.findItem(R.id.hide_system).setVisible(showSystem)
    menu.findItem(R.id.show_overlay).setVisible(showSystem && !showOverlay)
    menu.findItem(R.id.hide_overlay).setVisible(showSystem && showOverlay)
  }

  /**
   * Called when user selects an item.
   *
   * @param list a [List<String>] of selected items.
   */
  private fun onListUpdate(packageName: String, isChecked: Boolean) {
    if (!isAdded() || packageName.isBlank()) return
    for (info in userInfos) {
      if (isChecked) {
        hideAppListUtils.addApp(requireContext(), packageName, info.identifier)
      } else {
        hideAppListUtils.removeApp(requireContext(), packageName, info.identifier)
      }
    }
    try {
      activityManager.forceStopPackage(packageName)
    } catch (ignored: Exception) {}
  }

  private fun getKey(): String {
    return Settings.Secure.HIDE_APPLIST
  }

  private fun refreshList() {
    if (!isAdded()) return
    var list =
      packageList
        .filter {
          if (!showSystem) {
            !it.applicationInfo!!.isSystemApp() &&
              !resources
                .getStringArray(R.array.hide_applist_hidden_apps)
                .asList()
                .contains(it.applicationInfo!!.packageName) &&
              !it.applicationInfo!!.packageName.contains("android.settings")
          } else {
            if (!showOverlay) {
              !resources
                .getStringArray(R.array.hide_applist_hidden_apps)
                .asList()
                .contains(it.applicationInfo!!.packageName) &&
                !it.applicationInfo!!.packageName.contains("android.settings") &&
                !it.applicationInfo!!.isResourceOverlay()
            } else {
              !resources
                .getStringArray(R.array.hide_applist_hidden_apps)
                .asList()
                .contains(it.applicationInfo!!.packageName) &&
                !it.applicationInfo!!.packageName.contains("android.settings")
            }
          }
        }
        .filter { getLabel(it).contains(searchText, true) }

    if (customFilter != null) {
      list = list.filter { customFilter!!(it) }
    }
    if (comparator != null) {
      list = list.sortedWith { a, b -> comparator!!(a, b) }
    }
    adapter.submitList(list.map { appInfoFromPackageInfo(it) })
  }

  private fun appInfoFromPackageInfo(packageInfo: PackageInfo): AppInfo {
    return AppInfo(
      packageInfo.applicationInfo!!.packageName,
      getLabel(packageInfo),
      packageInfo.applicationInfo!!.isSystemApp()
    )
  }

  private fun getLabel(packageInfo: PackageInfo) =
    packageInfo.applicationInfo!!.loadLabel(packageManager).toString()

  private inner class AppListAdapter : ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {
    private val selectedIndices = mutableSetOf<Int>()
    private var initialList = getInitialCheckedList().toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
      AppListViewHolder(layoutInflater.inflate(R.layout.hide_applist_list_item, parent, false))

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
      getItem(position).let {
        holder.label!!.text = it.label
        holder.packageName!!.text = it.packageName
        holder.itemView!!.setOnClickListener {
          if (selectedIndices.contains(position)) {
            selectedIndices.remove(position)
            onListUpdate(holder.packageName!!.text.toString(), false)
          } else {
            selectedIndices.add(position)
            onListUpdate(holder.packageName!!.text.toString(), true)
          }
          notifyItemChanged(position)
        }
        if (initialList.contains(it.packageName)) {
          initialList.remove(it.packageName)
          selectedIndices.add(position)
        }
        holder.checkBox!!.isChecked = selectedIndices.contains(position)
      }
    }

    override fun submitList(list: List<AppInfo>?) {
      initialList = getInitialCheckedList().toMutableList()
      selectedIndices.clear()
      super.submitList(list)
    }
  }

  private class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: ImageView? = itemView.findViewById(R.id.app_icon)
    val label: TextView? = itemView.findViewById(R.id.app_label)
    val packageName: TextView? = itemView.findViewById(R.id.app_package)
    val checkBox: CheckBox? = itemView.findViewById(R.id.app_checkbox)
  }

  data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean
  )
}
