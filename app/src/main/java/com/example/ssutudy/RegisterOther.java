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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ssutudy.databinding.ActivityRegisterOtherBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RegisterOther extends AppCompatActivity {
    ActivityRegisterOtherBinding binding;
    private FirebaseUser firebaseUser;
    private FirebaseAuth mFirebaseAurth;        //파이어베이스 인증
    private DatabaseReference mDatabaseRef;     //실시간 데이터베이스

    private EditText tutorAccount;
    private EditText mUserAccount;              //회원가입 이메일
    private EditText mPassword;                 //회원가입 비밀번호
    private Button mRegisterBtn;                //회원가입 버튼
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterOtherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFirebaseAurth = FirebaseAuth.getInstance();
        FirebaseUser user = mFirebaseAurth.getCurrentUser();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("슈터디");

        tutorAccount = binding.OtherTeacherAccount;
        mUserAccount = binding.OtherUserAccount;
        mPassword = binding.OtherPassword;
        mRegisterBtn = binding.OtherRegisterButton;

        //역할 정보 받기
        Intent intent = getIntent();
        String role = intent.getStringExtra("role");

        //선생님 계정 입력 시 가이드 텍스트 띄우기
        tutorAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.guideText.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.guideText.setVisibility(View.VISIBLE);
                Log.d("JH", "$RegisterOther : show email guide");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        //뒤로가기 버튼 누르면
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterOther.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });

        //회원가입 버튼 누르면
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("JH", "$RegisterOther : register button clicked");
                //회원가입 정보 str
                String strAccount = mUserAccount.getText().toString().trim();
                String strPwd = mPassword.getText().toString().trim();
                String tutorEmail = tutorAccount.getText().toString().trim();

                //입력이 null 이면
                if(TextUtils.isEmpty(strAccount)|TextUtils.isEmpty(strPwd)|TextUtils.isEmpty(tutorEmail)){
                    binding.errorText.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        binding.errorText.setVisibility(View.INVISIBLE);
                    }, 2000);
                    Log.d("JH", "$RegisterOther : email or password is null (show error message)");
                    return;
                }

                //선생님 계정이 등록되었을 때만
                DoesTutorAccountExist(tutorEmail, exists -> {
                    if (exists) {
                        Log.d("JH", "$RegisterOther : tutor account exist..");

                        //Firebase Auth 진행
                        mFirebaseAurth.createUserWithEmailAndPassword(strAccount, strPwd)
                                .addOnCompleteListener(RegisterOther.this,
                                        new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                //회원 가입 성공하면
                                                if (task.isSuccessful()) {
                                                    Log.d("JH", "$RegisterOther : successfully sign up ");

                                                    firebaseUser = mFirebaseAurth.getCurrentUser();
                                                    UserAccountInfo account = new UserAccountInfo(firebaseUser.getUid(),
                                                            firebaseUser.getEmail(), strPwd, role, tutorEmail);
                                                    //데이터 쓰기
//                                                    mDatabaseRef.child("Users").child(firebaseUser.getUid()).setValue(account);
                                                    db = FirebaseFirestore.getInstance();
                                                    db.collection("accounts").document(firebaseUser.getUid()).set(account);
                                                    Log.d("JH", "$RegisterOther : data written");

                                                    Intent intent = new Intent(RegisterOther.this, LoginActivity.class);
                                                    startActivity(intent);
                                                    finish();
                                                    Log.d("JH", "$RegisterOther : go to login activity..");

                                                }
                                                //회원가입 실패하면 오류메시지
                                                else {
                                                    TextView errorMsg = binding.errorText;

                                                    try {
                                                        throw task.getException();
                                                    } catch (FirebaseAuthUserCollisionException e) {
                                                        // 아이디 중복
                                                        errorMsg.setText("아이디가 중복됩니다. 다른 아이디를 입력하세요.");
                                                    } catch (Exception e) {
                                                        // 비밀번호 조건
                                                        if(strPwd.length()<7){
                                                            errorMsg.setText("비밀번호는 7자리 이상 입력해야 합니다.");
                                                        } else {
                                                            binding.errorText.setText("회원가입에 실패했습니다. 다시 시도하세요.");
                                                            Log.d("JH", "$RegisterOther : error detected");
                                                        }
                                                    }

                                                    binding.errorText.setVisibility(View.VISIBLE);
                                                    new Handler().postDelayed(() -> {
                                                        binding.errorText.setVisibility(View.INVISIBLE);
                                                        Log.d("JH", "$RegisterOther : sign up failed (show error message)");
                                                    }, 2000);

                                                }
                                            }
                                        });
                    } else {
                        //선생님의 계정이 존재하지 않으면 오류메시지
                        Log.d("JH", "$RegisterOther : tutor account does not exist..");
                        binding.errorTutorText.setVisibility(View.VISIBLE);
                        new Handler().postDelayed(()-> {
                            binding.errorTutorText.setVisibility(View.INVISIBLE);
                        }, 2000);

                    }

                });
            }
        });
    }
    private void DoesTutorAccountExist(String tutorEmail, Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Firestore Query: Check if a document with the given teacherId exists
        db.collection("accounts")
                .whereEqualTo("teacherId", tutorEmail)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                // At least one document with the given tutorEmail exists
                                callback.onResult(true);
                            } else {
                                // No document matches the tutorEmail
                                callback.onResult(false);
                            }
                        } else {
                            // Query failed; handle the error
                            Log.e("Firestore", "Error checking tutor account existence", task.getException());
                            callback.onResult(false);
                        }
                    }
                });
    }
    public interface Callback{
        void onResult(boolean exists);
    }
    /*private void setLoggedOutState(boolean isLoggedOut) {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedOut", isLoggedOut);
        editor.apply();
    }*/
}