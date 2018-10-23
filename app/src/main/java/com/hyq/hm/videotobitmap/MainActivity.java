package com.hyq.hm.videotobitmap;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.hyq.hm.gifencoder.GifExtractor;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HMSDK/test_1535007031957.gif";
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        GifExtractor gifExtractor = new GifExtractor(this, Environment.getExternalStorageDirectory().getAbsolutePath() + "/HMSDK/video/1535007031957.mp4");
        gifExtractor.encoder(path, 0, 10 * 1000, 15,15, 320, 240);
    }

}
