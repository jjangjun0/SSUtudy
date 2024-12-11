package com.example.ssutudy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ssutudy.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    private String role;
    private Button registerBtn;
    private Button loginBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference mDataRef;
    private EditText mUserAccount;
    private EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //SelectRole -> 역할 뭔지 전달 받기
        Intent intent = getIntent();
        role = intent.getStringExtra("role");

        //초기화
        mAuth = FirebaseAuth.getInstance();
        mDataRef = FirebaseDatabase.getInstance().getReference();

        mUserAccount = binding.userAccount;
        mPassword = binding.password;

        /*만약 로그인 되어 있는 상태라면, 그 아이디를 기억해 로그인 하지 않고 바로 HomeActivity로 이동한다.
        로그아웃 하게 되면 이 동작은 자연스럽게 작동하지 않게 된다.*/

        // Home 화면에서 userEmail 을 가져와야 한다.
        SharedPreferences sharedPref = getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
        String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
        Log.d("JH", "LoginActivity's already set Email : " + user_email);
        // user_email != "noEmail" 인 상태는 로그인을 해서 HomeActivity에 들어갔다는 뜻이고, 아직 로그아웃을 하지 않은 것이다.
        if (user_email != "noEmail") {
            Intent goMainActivityIntent = new Intent(LoginActivity.this, HomeActivity.class);
            //홈화면으로 역할 정보 보내주기.
            goMainActivityIntent.putExtra("role", role);
            startActivity(goMainActivityIntent);
            finish(); //현재 액티비티 destroy
        }

        //회원가입 누를 때
        registerBtn = binding.ToRegisterButton;
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("JH", "$LoginActivity : register button clicked");
                //회원가입 화면으로 이동
                if (role.equals("teacher")) { //선생님이면
                    startActivity(new Intent(LoginActivity.this, RegisterTeacher.class)
                            .putExtra("role", role));
                } else {//학부모나 학생이면
                    startActivity(new Intent(LoginActivity.this, RegisterOther.class)
                            .putExtra("role", role));
                }
                Log.d("JH", "$LoginActivity : go to RegisterActivity..");
            }
        });

        //바로 로그인 할 때
        loginBtn = findViewById(R.id.LoginButton);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("JH", "$LoginActivity : login button clicked");
                //로그인 요청
                String strAccount = mUserAccount.getText().toString().trim();
                String strPwd = mPassword.getText().toString().trim();

                //이메일, 비밀번호 null이면
                if (TextUtils.isEmpty(strAccount) | TextUtils.isEmpty(strPwd)) {
                    TextView errorMsg = binding.LoginErrorText;
                    errorMsg.setText("잘못된 형식입니다. 다시 시도하세요");
                    Log.d("JH", "$LoginActivity : email or password is null (show error message)");

                    errorMsg.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        errorMsg.setVisibility(View.INVISIBLE);
                    }, 2000);
                    return;
                }
                mAuth.signInWithEmailAndPassword(strAccount, strPwd).addOnCompleteListener(LoginActivity.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {//로그인 성공하면

                                    // checkUserRoleAndNavigate(role);
                                    // home activity 로 화면 전환
                                    Log.d("JH", "$LoginActivity : successfully login. go to home activity...");
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    //홈화면으로 역할 정보 보내주기.
                                    intent.putExtra("role", role);
                                    startActivity(intent);
                                    finish(); //현재 액티비티 destroy
                                } else { //어떤 이유로든 로그인이 잘못되면 에러메시지 띄우기
                                    TextView errorMsg = binding.LoginErrorText;
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthInvalidUserException e) {
                                        errorMsg.setText("등록되지 않은 계정입니다. 다시 입력하세요.");
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        String errorCode = e.getErrorCode();
                                        switch (errorCode) {
                                            case "ERROR_INVALID_EMAIL":
                                                errorMsg.setText("잘못된 이메일 형식입니다.");
                                                break;
                                            case "ERROR_WRONG_PASSWORD":
                                                errorMsg.setText("비밀번호가 틀렸습니다.");
                                                break;
                                        }
                                    } catch (Exception e) {
                                        errorMsg.setText("로그인에 실패했습니다. 다시 시도하세요.");
                                        Log.e("JH", "login error detected");
                                    }
                                    //오류메시지 2초간
                                    errorMsg.setVisibility(View.VISIBLE);
                                    new Handler().postDelayed(() -> {
                                        errorMsg.setVisibility(View.INVISIBLE);
                                        Log.d("JH", "$LoginActivity : login failed (show error message)");
                                    }, 2000);
                                }
                            }
                        });
            }
        });
    }

    private void checkUserRoleAndNavigate(String localRole) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDataRef = FirebaseDatabase.getInstance().getReference();

        // 현재 로그인된 사용자 가져오기
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e("LoginActivity", "No user logged in");
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            // SelectRole로 이동
            startActivity(new Intent(LoginActivity.this, SelectRole.class));
            finish();
            return;
        }

        String userId = ((FirebaseUser) currentUser).getUid();

        // Firebase에서 현재 사용자의 "role" 가져오기
        mDataRef.child("Users").child(userId).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String firebaseRole = dataSnapshot.getValue(String.class); // Firebase에서 role 값 읽기

                            Log.d("JH", "Firebase Role: " + firebaseRole);
                            Log.d("JH", "Local Role: " + localRole);

                            // Firebase의 role과 localRole 비교
                            if (firebaseRole != null && firebaseRole.equals(localRole)) {
                                Log.d("JH", "Role matches! Proceed to HomeActivity.");
                                // HomeActivity로 이동
                                startActivity(new Intent(LoginActivity.this, HomeActivity.class)
                                        .putExtra("role", localRole));
                                finish();
                            } else {
                                Log.d("JH", "Role mismatch! Redirecting to SelectRole.");
                                Toast.makeText(LoginActivity.this, "역할이 일치하지 않습니다. 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
                                // SelectRole로 이동
                                startActivity(new Intent(LoginActivity.this, SelectRole.class));
                                finish();
                            }
                        } else {
                            Log.e("JH", "No role found for user.");
                            Toast.makeText(LoginActivity.this, "역할 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, SelectRole.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("LoginActivity", "Database error: " + databaseError.getMessage());
                        Toast.makeText(LoginActivity.this, "서버 오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}