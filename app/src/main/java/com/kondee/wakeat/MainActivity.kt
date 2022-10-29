package com.kondee.wakeat

import androidx.appcompat.app.AppCompatActivity
import com.kondee.wakeat.MainActivity.onOptionMenuCreated
import com.kondee.wakeat.MainActivity.onOptionItemSelected
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.kondee.wakeat.R
import android.os.Parcelable
import com.kondee.wakeat.MainFragment
import android.content.Intent
import com.kondee.wakeat.ForegroundLocationService
import com.kondee.wakeat.service.ServiceConstant
import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import com.kondee.wakeat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    private var menuCreatedListener: onOptionMenuCreated? = null
    private var itemSelectedListener: onOptionItemSelected? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initInstance()
    }

    private fun initInstance() {
        setSupportActionBar(binding!!.toolBar)
        val parcelable = intent.getParcelableExtra<Parcelable>("parcelable")
        supportFragmentManager.beginTransaction()
            .replace(binding!!.contentContainer.id, MainFragment.newInstance(parcelable), "MainFragment")
            .commit()
        val intent = Intent(this, ForegroundLocationService::class.java)
        intent.action = ServiceConstant.STOPFOREGROUND_ACTION
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x1 && resultCode == RESULT_OK) {
            if (supportFragmentManager.findFragmentById(R.id.contentContainer) is MainFragment) {
                (supportFragmentManager.findFragmentById(R.id.contentContainer) as MainFragment?)!!.updateCameraPosition()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.location, menu)
        if (menuCreatedListener != null) {
            menuCreatedListener!!.onMenuCreated(menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.location) {
            if (itemSelectedListener != null) {
                itemSelectedListener!!.onMenuSelected()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    interface onOptionMenuCreated {
        fun onMenuCreated(menu: Menu?)
    }

    fun setOnOptionMenuCreatedListener(menuCreatedListener: onOptionMenuCreated?) {
        this.menuCreatedListener = menuCreatedListener
    }

    interface onOptionItemSelected {
        fun onMenuSelected()
    }

    fun setOnOptionItemSelectedListener(itemSelectedListener: onOptionItemSelected?) {
        this.itemSelectedListener = itemSelectedListener
    }
}