package com.kondee.wakeat;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.kondee.wakeat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Kondee";
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0x1 && resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult: OK");
            if (getSupportFragmentManager().findFragmentById(R.id.contentContainer) instanceof MainFragment) {
                ((MainFragment) getSupportFragmentManager().findFragmentById(R.id.contentContainer)).updateCameraPosition();
            }
        }
    }
}
