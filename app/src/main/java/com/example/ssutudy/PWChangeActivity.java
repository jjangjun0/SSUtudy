package com.example.ssutudy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ssutudy.databinding.ActivityPwchangeBinding;
import com.example.ssutudy.databinding.ActivityRegisterTeacherBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PWChangeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPwchangeBinding binding = ActivityPwchangeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("JH", "$PWChangeActivity");

        // Firebase 인증
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

        //실시간 데이터베이스
        DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference("슈터디");


        // 뒤로가기 버튼
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PWChangeActivity.this, HomeActivity.class);
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
                Log.d("JH", "back button clicked");
                finish();
            }
        });

        // 비밀번호 재설정 버튼
        binding.ResetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //현재 유저 정보와 입력한 비밀번호 받아두기
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String newPassword = binding.AnotherPassword.getText().toString();

                //비밀번호 nullpointException 처리
                if (newPassword.isEmpty()){
                    binding.errorText.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        binding.errorText.setVisibility(View.INVISIBLE);
                    }, 2000);
                    Log.d("JH", "$PWChangeActivity : password is null (show error message");
                    return;
                }

                boolean rightAccount = true;
                //본인 계정이 아닌 계정을 입력하면
                if(!user.getEmail().equals(binding.UserAccount.getText().toString().trim())){
                    binding.accountError.setText("올바르지 않은 계정입니다. 다시 입력하세요.");
                    binding.accountError.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        binding.errorText.setVisibility(View.INVISIBLE);
                    }, 2000);
                    Log.d("JH", "$PWChangeActivity : wrong account (show error message");
                    rightAccount=false;
                    return;
                }

                //유저가 존재하고, 본인 계정을 잘 입력했다면
                if (user != null && rightAccount) {
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    //비밀번호를 성공적으로 업데이트 했다면
                                    if (task.isSuccessful()) {
                                        Log.d("JH", "$PWChangeActivity : password updated");

                                        //데이터베이스 비밀번호 수정 ( 예 : changedPassword (new) )
                                        mDatabaseRef.child("Users").child(user.getUid()).child("password")
                                                .setValue(newPassword + " (new)")
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> dbTask) {
                                                        if (dbTask.isSuccessful()) {

                                                            Log.d("JH", "$PWChangeActivity : password in database updated");
                                                            Toast.makeText(PWChangeActivity.this, "변경되었습니다", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(PWChangeActivity.this, HomeActivity.class));
                                                        } else {
                                                            Log.d("JH", "$PWChangeActivity : password in database not updated");
                                                        }
                                                    }
                                                });
                                    } else {
                                        new Handler().postDelayed(() -> {
                                            binding.updateErrorText.setVisibility(View.VISIBLE);
                                        }, 2000);
                                        Log.d("JH", "$PWChangeActivity : update fail");
                                    }
                                }
                            });
                } else {
                    Log.d("JH", "$PWChangeActivity : no user exist");
                    binding.updateErrorText.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}