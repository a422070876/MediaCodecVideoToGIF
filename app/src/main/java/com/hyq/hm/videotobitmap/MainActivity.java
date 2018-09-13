package com.hyq.hm.videotobitmap;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hyq.hm.gifencoder.GifExtractor;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GifExtractor gifExtractor = new GifExtractor(this, Environment.getExternalStorageDirectory().getAbsolutePath() + "/HMSDK/video/1524204109321.mp4");
        gifExtractor.encoder(Environment.getExternalStorageDirectory().getAbsolutePath() + "/HMSDK/test_1524204109321.gif", 0, 15 * 1000, 15, 320, 240);
    }

}
