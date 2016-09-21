package com.project.macys.macyssdcard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Intent serviceIntent;
    MainActivity mainActivity;

    MyReceiver myReceiver;
    ProgressBar scanProgress;
    LinearLayout listLayout;
    TextView avgSizeText;
    ListView lv1;
    ListView lv2;


    static final String AVG_FILE_SIZE = "avg_file_size";
    static final String LARGEST_FILES_LIST = "largest_file_list";
    static final String FREQUENCY_LIST = "freq_list";

    ArrayList<String> largestFilesList;
    ArrayList<String> frequencyList;
    String avg_file_size;

    Menu menu;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        avgSizeText = (TextView)findViewById(R.id.average_file_size);
        listLayout = (LinearLayout) findViewById(R.id.list_view_layout);
        scanProgress = (ProgressBar) findViewById(R.id.progressBar);
        btn = (Button)findViewById(R.id.button);
        lv1 = (ListView)findViewById(R.id.listView1);
        lv2 = (ListView)findViewById(R.id.listView2);


        if(savedInstanceState != null) {
            largestFilesList = savedInstanceState.getStringArrayList(LARGEST_FILES_LIST);
            frequencyList = savedInstanceState.getStringArrayList(FREQUENCY_LIST);
            avg_file_size = savedInstanceState.getString(AVG_FILE_SIZE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(largestFilesList != null && frequencyList != null && avg_file_size != null){
            displayFileData();
        }

        if(FileScannerService.isServiceRunning){
            scanProgress.setVisibility(View.VISIBLE);
            btn.setText("Stop Scan");
        }

        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FileScannerService.PROGRESS_UPDATE);
        registerReceiver(myReceiver, intentFilter);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(AVG_FILE_SIZE, avg_file_size);
        outState.putStringArrayList(LARGEST_FILES_LIST, largestFilesList);
        outState.putStringArrayList(FREQUENCY_LIST, frequencyList);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        myReceiver = null;
        stopService(new Intent(this,FileScannerService.class));
    }

    public void startService(View v){
        Button btn = (Button)v;

        scanProgress.setVisibility(View.VISIBLE);
        if(btn.getText().toString().equals("Start Scan")) {
            serviceIntent = new Intent(this, FileScannerService.class);
            startService(serviceIntent);
            btn.setText("Stop Scan");
        }
        else
        {
            stopService(new Intent(this,FileScannerService.class));
            btn.setText("Start Scan");
            scanProgress.setVisibility(View.INVISIBLE);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        if(largestFilesList == null || frequencyList == null || avg_file_size == null)
            menu.findItem(R.id.action_share).setEnabled(false);
        else
            menu.findItem(R.id.action_share).setOnMenuItemClickListener(onMenuItemClickListener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void toggleVisibility(int visiblity){
        avgSizeText.setVisibility(visiblity);
        listLayout.setVisibility(visiblity);
    }

    public void displayFileData(){

        String text = "Average File Size: " + String.valueOf(avg_file_size);
        avgSizeText.setText(text);

        lv1.setAdapter(new ListViewAdapter(this, R.layout.file_info_layout, largestFilesList));
        lv2.setAdapter(new ListViewAdapter(this, R.layout.file_info_layout, frequencyList));
        toggleVisibility(View.VISIBLE);
    }


    public String getStringFormattedData(){

        StringBuilder result = new StringBuilder();

        if(largestFilesList != null){
            result.append("################ \n");
            result.append("10 Largest Files \n");
            result.append("################ \n");
            for(String s:largestFilesList){
                String[] split = s.split(";");
                result.append("\u27A4"+ " "+split[0] + "\n");
                result.append(split[1] + "\n");
            }
        }

        if(frequencyList != null){
            result.append("################ \n");
            result.append("File frequencies \n");
            result.append("################ \n");
            for(String s:frequencyList){
                String[] split = s.split(";");
                result.append("\u27A4"+ " "+split[0] + "\n");
                result.append(split[1] + "\n");
            }
        }

        if(avg_file_size != null){
            result.append("################ \n");
            result.append("\u27A4"+ " "+"Avg file size: "+avg_file_size + "\n");
            result.append("################ \n");
        }
    return new String(result);
    }


    MenuItem.OnMenuItemClickListener onMenuItemClickListener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            String s = getStringFormattedData();
            sendIntent.putExtra(Intent.EXTRA_TEXT, s);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share via"));
            return false;
        }
    };

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub

            btn.setText("Start Scan");
            scanProgress.setVisibility(View.INVISIBLE);

            largestFilesList = (ArrayList<String>) arg1.getStringArrayListExtra("LARGEST_FILE_LIST");
            frequencyList = (ArrayList<String>) arg1.getStringArrayListExtra("FREQUENCY_LIST");
            avg_file_size = arg1.getStringExtra("AVG_SIZE");

            if(largestFilesList != null && frequencyList != null && avg_file_size != null){
                displayFileData();
                menu.findItem(R.id.action_share).setEnabled(true);
                menu.findItem(R.id.action_share).setOnMenuItemClickListener(onMenuItemClickListener);
            }
        }
    }
}
