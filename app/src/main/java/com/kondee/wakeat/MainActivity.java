package com.kondee.wakeat;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.kondee.wakeat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        initInstance();
    }

    private void initInstance() {

        setSupportActionBar(binding.toolBar);

        getSupportFragmentManager().beginTransaction()
                .replace(binding.contentContainer.getId(), MainFragment.newInstance(), "MainFragment")
                .commit();
    }
}
