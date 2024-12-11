package com.example.ssutudy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ssutudy.databinding.ActivityRegisterTeacherBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterTeacher extends AppCompatActivity {

    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegisterTeacherBinding binding = ActivityRegisterTeacherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //파이어베이스 인증
        FirebaseAuth mFirebaseAurth = FirebaseAuth.getInstance();
        //실시간 데이터베이스

        //회원가입 이메일
        EditText mUserAccount = binding.TeacherUserAccount;
        //회원가입 비밀번호
        EditText mPassword = binding.TeacherPassword;
        //회원가입 버튼
        Button mRegisterBtn = binding.TeacherRegisterButton;

        //역할 정보 받기
        Intent intent = getIntent();
        String role = intent.getStringExtra("role");

        //본인 이메일 입력 시 가이드 텍스트 띄우기
        mUserAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.guideText.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.guideText.setVisibility(View.VISIBLE);
                Log.d("JH", "$RegisterTeacher : show email guide");
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //뒤로가기 버튼 누르면
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterTeacher.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        //회원가입 버튼 누르면
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("JH", "$RegisterTeacher : register button clicked");
                //회원가입 정보 str
                String strAccount = mUserAccount.getText().toString();
                String strPwd = mPassword.getText().toString();

                if(TextUtils.isEmpty(strAccount)|TextUtils.isEmpty(strPwd)){
                    binding.errorText.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        binding.errorText.setVisibility(View.INVISIBLE);
                    }, 2000);
                    Log.d("JH", "$RegisterTeacher : email or password is null (show error message)");
                    return;
                }

                //Firebase Auth 진행
                mFirebaseAurth.createUserWithEmailAndPassword(strAccount,
                        strPwd).addOnCompleteListener(RegisterTeacher.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    Log.d("JH", "$RegisterTeacher : successfully sign up ");

                                    FirebaseUser firebaseUser = mFirebaseAurth.getCurrentUser();
                                    UserAccountInfo account = new UserAccountInfo(firebaseUser.getUid(),
                                            firebaseUser.getEmail(),strPwd,role, firebaseUser.getEmail());

                                    //데이터 쓰기
//                                    mDatabaseRef.child("Users").child(firebaseUser.getUid()).setValue(account);

                                    db = FirebaseFirestore.getInstance();
                                    db.collection("accounts").document(firebaseUser.getUid()).set(account);


                                    Log.d("JH", "$RegisterTeacher : data written");

                                    Intent intent = new Intent(RegisterTeacher.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                    Log.d("JH", "$RegisterTeacher : go to login activity..");


                                }else{
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthUserCollisionException e) {
                                        // 중복 이메일 예외 처리
                                        binding.errorText.setText("아이디가 중복됩니다. 다른 아이디를 입력하세요.");
                                        Log.d("JH", "$RegisterTeacher : error detected - duplicated email(id)");
                                    } catch (Exception e) {
                                        // 일반적인 예외 처리
                                        binding.errorText.setText("회원가입에 실패했습니다. 다시 시도하세요.");
                                        Log.d("JH", "$RegisterTeacher : error detected ");

                                    }


                                    binding.errorText.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        binding.errorText.setVisibility(View.INVISIBLE);
                                        Log.d("JH", "$RegisterTeacher : sign up failed (show error message)");
                                    }, 2000);

                                }
                            }
                        });
            }
        });
    }

    /*private void setLoggedOutState(boolean isLoggedOut) {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedOut", isLoggedOut);
        editor.apply();
    }*/
}