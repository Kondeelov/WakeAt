package com.kondee.wakeat;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.kondee.wakeat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Kondee";
    ActivityMainBinding binding;
    private onOptionMenuCreated menuCreatedListener;
    private onOptionItemSelected itemSelectedListener;

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
            if (getSupportFragmentManager().findFragmentById(R.id.contentContainer) instanceof MainFragment) {
                ((MainFragment) getSupportFragmentManager().findFragmentById(R.id.contentContainer)).updateCameraPosition();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location, menu);

        if (menuCreatedListener != null) {
            menuCreatedListener.onMenuCreated(menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.location) {
            if (itemSelectedListener != null) {
                itemSelectedListener.onMenuSelected();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public interface onOptionMenuCreated {
        void onMenuCreated(Menu menu);
    }

    public void setOnOptionMenuCreatedListener(onOptionMenuCreated menuCreatedListener) {
        this.menuCreatedListener = menuCreatedListener;
    }

    public interface onOptionItemSelected {
        void onMenuSelected();
    }

    public void setOnOptionItemSelectedListener(onOptionItemSelected itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }

}
