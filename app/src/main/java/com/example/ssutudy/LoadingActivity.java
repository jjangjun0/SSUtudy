package com.example.ssutudy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // 로딩 화면의 루트 레이아웃
        LinearLayout rootLayout = findViewById(R.id.main);

        // 페이드아웃 애니메이션 생성
        new Handler().postDelayed(() -> {
            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f); //투명도
            fadeOut.setDuration(800); // 지속시간
            fadeOut.setFillAfter(true); // 애니메이션 종료 후 상태 유지

            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    startActivity(new Intent(LoadingActivity.this, SelectRole.class));
                    finish();
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            rootLayout.startAnimation(fadeOut);

        }, 2000);
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(LoadingActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}
