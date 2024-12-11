package com.example.ssutudy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ssutudy.databinding.ActivitySelectRoleBinding;

public class SelectRole extends AppCompatActivity { //사용자 선택화면
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySelectRoleBinding binding= ActivitySelectRoleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LinearLayout teacherButton = binding.teacherButton;
        LinearLayout parentButton = binding.parentButton;
        LinearLayout studentButton = binding.studentButton;

        /*만약 로그인 되어 있는 상태라면, 그 아이디를 기억해 역할을 선택하지 않고 바로 LoginActivity로 이동한다.
        로그아웃 하게 되면 이 동작은 자연스럽게 작동하지 않게 된다.*/

        // "SelectRole"

        // Home 화면에서 userEmail 을 가져와야 한다.
        SharedPreferences sharedPref = getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
        String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
        Log.d("JH", "SelectRole's already set Email : " + user_email);
        // user_email != "noEmail" 인 상태는 로그인을 해서 HomeActivity에 들어갔다는 뜻이고, 아직 로그아웃을 하지 않은 것이다.
        if (user_email != "noEmail") {
            // 바로 HomeActivity 로 점프
            Intent goMainActivityIntent = new Intent(SelectRole.this, HomeActivity.class);
            //홈화면으로 역할 정보 보내주기.
            goMainActivityIntent.putExtra("role",role);
            startActivity(goMainActivityIntent);
            finish(); //현재 액티비티 destroy
        }

        teacherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                role = "teacher";
                Intent intent = new Intent(SelectRole.this, LoginActivity.class);
                intent.putExtra("role",role);
                SelectRole.this.startActivity(intent);
                finish();
            }
        });

        parentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                role = "parent";
                Intent intent = new Intent(SelectRole.this, LoginActivity.class);
                intent.putExtra("role",role);
                startActivity(intent);
                finish();
            }
        });

        studentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                role = "student";
                Intent intent = new Intent(SelectRole.this, LoginActivity.class);
                intent.putExtra("role",role);
                startActivity(intent);
                finish();
            }
        });
    }
}