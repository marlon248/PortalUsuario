package com.marlon.portalusuario.view.fragments.connectivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.marlon.portalusuario.R
import com.marlon.portalusuario.nauta.ui.WifiEtecsaFragment
import com.marlon.portalusuario.view.Fragments.SitiosNacionalesFragment
import javax.inject.Inject

class ConnectivityFragment @Inject constructor(
    private val wifiEtecsaFragment: WifiEtecsaFragment
): Fragment() {
    private var tabLayout: TabLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connectivity, container, false)
        tabLayout = view.findViewById(R.id.simpleTabLayout)
        //
        val wifiEtecsaTab = tabLayout?.newTab()
        if (wifiEtecsaTab != null) {
            wifiEtecsaTab.text = resources.getString(R.string.connectivity)
        }
        wifiEtecsaTab?.setIcon(R.drawable.round_wifi_lock_24)
        if (wifiEtecsaTab != null) {
            tabLayout?.addTab(wifiEtecsaTab, true)
        }
        //
//        TabLayout.Tab nautaTab = tabLayout.newTab();
//        nautaTab.setText("Portal Nauta");
//        nautaTab.setIcon(R.drawable.ic_nauta);
//        tabLayout.addTab(nautaTab);
        //
        val nationalTab = tabLayout?.newTab()
        if (nationalTab != null) {
            nationalTab.text = resources.getString(R.string.national_sites)
        }
        nationalTab?.setIcon(R.drawable.round_web_24)
        if (nationalTab != null) {
            tabLayout?.addTab(nationalTab)
        }
        //
        tabLayout?.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> setFragment(wifiEtecsaFragment)
                    1 -> setFragment(SitiosNacionalesFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        setFragment(wifiEtecsaFragment)
        return view
    }

    fun setFragment(fragment: Fragment?) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.simpleFrameLayout, fragment!!)
            .commit()
    }
}