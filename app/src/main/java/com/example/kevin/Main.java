package com.example.kevin;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

public class Main extends Activity {

    private Button enter;



    private VideoView video;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_app);


        videoinit();




          video.start();


//        MediaController controller = new MediaController(MainActivity.this);
//        video.setMediaController(controller);


        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                String uri = "android.resource://" + getPackageName() + "/" + R.raw.intro;


                video.setVideoURI(Uri.parse(uri));
                video.start();
            }
        });
//        image=(ImageView)findViewById(R.id.image);
//
//        Animation alphaAnimation=new AlphaAnimation(1, (float) 0.0);
//
//        alphaAnimation.setDuration(5000);//设置动画持续时间为3秒
//
//        alphaAnimation.setFillAfter(true);//设置动画结束后保持当前的位置（即不返回到动画开始前的位置）
//
//        image.startAnimation(alphaAnimation);







    Button enter=(Button)findViewById(R.id.enter);

    enter.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i=new Intent(Main.this,MainActivity.class);
            startActivity(i);
            finish();
        }
    });
}

    private void videoinit() {

        video = (VideoView) findViewById(R.id.video);
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.intro;
        video.setVideoURI(Uri.parse(uri));
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        video.setLayoutParams(layoutParams);


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        video.start();
    }
}

