package com.example.ssutudy;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.fragment.app.Fragment;

import com.example.ssutudy.databinding.NavHeaderBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mFirebaseAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private BottomNavigationView bottomNavigationView;

    private AlertDialog inputUserNameDialog;
    private AlertDialog checkTrueDialog;

    private long initTime;
    SharedPreferences sharedPref;
    SharedPreferences sharedPref2;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("JH", "$ HomeActivity");

        // data 저장할 파일 등록 - "startDay" (과외 시작일), "userName" (사용자 이름), "userEmail" (사용자 이메일), "userRole" (사용자 역할)
        sharedPref = getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
        // data 저장된 파일 등록
        sharedPref2 = getSharedPreferences("ClassFragment", Context.MODE_PRIVATE); // documentId 가져오기
        // 앱 첫 실행 시, 과외 시작일 지정
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        /*dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul")); -> 한국 시간대, 서울 지역 특성에 맞추어 시간을 받아옴*/
        String todayDate = dateFormat.format(date);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        //toolbar 및 drawerLayout 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        //nav_header 인사말
        View headerView = navigationView.getHeaderView(0); // 첫 번째 헤더 가져오기
        TextView navHeaderText = headerView.findViewById(R.id.nav_header_text); // 헤더 내부의 TextView ID
        mFirebaseAuth = FirebaseAuth.getInstance();
        navHeaderText.setText(mFirebaseAuth.getCurrentUser().getEmail()+"님\n환영합니다");

        /*전에 입력한 데이터 있나? 있으면 얻어 오기*/
        String start_day = sharedPref.getString("startDay", todayDate); // default: 실행 한 날짜
        // userEmail sharedPref 에 등록
        String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
        // userName sharedPref 에 등록
        String user_name = sharedPref.getString("userName", "noName"); // default: "noName"

        //Log.d("JH", "first " + start_day);
        Log.d("JH", "HomeActivity's already set Email : " + user_email);
        if (user_email == "noEmail") {
            Log.d("JH", "new getEmail -> " + mFirebaseAuth.getCurrentUser().getEmail());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("userEmail", mFirebaseAuth.getCurrentUser().getEmail() + "");
            editor.apply();
        }
        if (user_name != "noName") {
            // 앱을 다시 실행한 경우
            Log.d("JH", "HomeActivity's already set Name : " + user_name);
            navHeaderText.setText(user_name+"님\n환영합니다");
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        //bottom navigation 설정
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new HomeFragment())
                    .commit();
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if(itemId == R.id.home_page){
                    Log.d("JH", "# Home_Page");
                    // fragment 매번 새롭게 생성
                    HomeFragment homeFragment = new com.example.ssutudy.HomeFragment();
                    Bundle bundle = new Bundle();

                    String fragment_start_day = sharedPref.getString("startDay", todayDate); // default: 실행 한 날짜
                    bundle.putString("startDay", fragment_start_day);
                    Log.d("JH", "now startDay : " + sharedPref.getString("startDay", "2022-4-16"));
                    homeFragment.setArguments(bundle);
                    // 전달
                    transferTo(homeFragment);
                    return true;
                }
                if(itemId == R.id.calendar_page){
                    Log.d("JH", "# Calendar_Page");
                    // 아직
                    transferTo(new CalendarFragment());
                    return true;
                }
                if(itemId == R.id.class_page){
                    Log.d("JH", "# Class_Page");
                    transferTo(new ClassFragment());
                    return true;
                }
                if(itemId == R.id.homework_page){
                    Log.d("JH", "# Homework_Page");
                    transferTo(new HWFragment());
                    return true;
                }
                if(itemId == R.id.chatting_page){
                    Log.d("JH", "# Chatting_Page");
                    String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"


                    // Classes 컬렉션에 가서 문서 필드의 emailId = user_email인 문서를 찾아가 그 문서의 필드 "role"값을 String으로 받아온다.

                    // Firestore 초기화
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    // Firestore에서 emailId가 user_email인 문서를 찾기
                    db.collection("accounts")
                            .whereEqualTo("emailId", user_email)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    // 문서를 가져옴
                                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                                    String role = document.getString("role"); // "role" 필드값 가져오기

                                    if (role != null) {
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putString("userRole", role);
                                        editor.apply();
                                    } else {
                                        Log.d("Firestore", "Role 필드가 없습니다.");
                                    }
                                } else {
                                    Log.d("Firestore", "해당 이메일에 해당하는 문서를 찾을 수 없습니다.");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "데이터를 가져오지 못했습니다: " + e.getMessage());
                            });
                    String currentRole = sharedPref.getString("userRole", "noRole"); // default : "noRole"
                    Log.d("JH", "currentRole : " + currentRole);

                    // ChattingFragment 로 이동
                    transferTo(new ChattingFragment());
                    Log.d("JH","$HomeActivity -> ChattingFragment");
                    return true;
                }
                return false;
            }
        });
        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) { }
        });
        // 첫 실행은 HomeFragment 가 실행 되어야 함.
        HomeFragment homeFragment = new HomeFragment();
        Bundle bundle = new Bundle();
        bundle.putString("startDay", start_day);
        homeFragment.setArguments(bundle);
        // 전달
        transferTo(homeFragment);
    }

    //drawer_menu 에 적힌 버튼이 눌릴 경우 frame 교체
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.user_nickname) {

            final EditText inputEditText = getEditText();

            inputUserNameDialog = new AlertDialog.Builder(this)
                    .setTitle("사용자 이름")
                    .setIcon(R.drawable.baseline_account_circle_24)
                    .setView(inputEditText)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String userName = inputEditText.getText().toString();
                            Log.d("JH", userName);
                            // 데이터 등록
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("userName", userName);
                            editor.apply();
                            // 변경할 사용자 이름 입력, // 헤더 내부의 TextView ID
                            View headerView = navigationView.getHeaderView(0);
                            TextView navHeaderText = headerView.findViewById(R.id.nav_header_text);
                            navHeaderText.setText(userName+"님\n환영합니다");
                        }
                    })
                    .setNegativeButton("취소", null)
                    .create();
            inputUserNameDialog.show();

        } else if (item.getItemId() == R.id.nav_change_password) {
            Log.d("JH", "password change page");
            startActivity(new Intent(this, PWChangeActivity.class));

        } else if (item.getItemId() == R.id.nav_start_day) {
            Log.d("JH", "start day input dialog");
            // 첫 과외 시작 날짜 입력 받기
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dateDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monOfYear, int dayOfMonth) {
                            String startDay = year + "-" + (monOfYear + 1) + "-" + dayOfMonth;
                            Log.d("JH", "calendarDialog : " + startDay);
                            // 데이터 등록
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("startDay", startDay);
                            editor.apply();

                            HomeFragment homeFragment = new com.example.ssutudy.HomeFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("startDay", startDay);  // SharedPreferences 대신 방금 설정한 값을 바로 전달
                            homeFragment.setArguments(bundle);
                            // 전달
                            transferTo(homeFragment);
                        }
                    }, year, month, day);

            dateDialog.show();

        } else if(item.getItemId() == R.id.nav_logout){
            // 로그 아웃 확인 받기
            checkTrueDialog = new AlertDialog.Builder(this)
                    .setMessage("로그아웃 하시겠습니까?")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 등록된 "userEmail" key-value 삭제
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.remove("userEmail");
                            editor.remove("userName");
                            editor.remove("startDay");
                            // 작동
                            editor.apply();

                            // 등록된 "documentId" 삭제
                            SharedPreferences.Editor editor2 = sharedPref2.edit();
                            editor2.remove("documentId");
                            editor2.apply();
                            Log.d("JH", "logout");

                            // 삭제되었는지 확인
                            String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
                            String user_name = sharedPref.getString("userName", "noName"); // default: "noName"
                            String start_day = sharedPref.getString("startDay", "2022-4-16"); // default: "2022-4-16"
                            Log.d("JH", "sharedPref' userEmail : " + user_email);
                            Log.d("JH", "sharedPref' userName : " + user_name);
                            Log.d("JH", "sharedPref' startDay : " + start_day);
                            String document_id = sharedPref2.getString("documentId", "noDocumentId");
                            Log.d("JH", " sharedPref2' documentId : " + document_id);
                            boolean isRemoved = editor2.commit(); // 동기적으로 삭제 확인
                            Log.d("JH", "documentId remove? : " + isRemoved);

                            Log.d("JH", "\n");

                            mFirebaseAuth = FirebaseAuth.getInstance();
                            mFirebaseAuth.signOut();
                            //setLoggedOutState(true);
                            startActivity(new Intent(HomeActivity.this, SelectRole.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK));
                            finish();
                        }
                    })
                    .setNegativeButton("취소", null)
                    .create();
            checkTrueDialog.show();

        } else if(item.getItemId() == R.id.nav_help){
            Log.d("JH", "for help page");
            startActivity(new Intent(HomeActivity.this, HelpActivity.class));
            // finish(); 도움말 화면에서 뒤로가기를 눌렀을 때, 앱이 종료되면 안된다.
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // 새로운 EditText 생성 함수 (자동 생성됨)
    private @NonNull EditText getEditText() {
        final EditText inputEditText = new EditText(this);
        inputEditText.setHint("이곳에 입력하세요");
        // enter 입력 받지 못하게 제한
        InputFilter filter1 = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String unAcceptableText = "\n";

                for (int i = start; i < end; i++) {
                    if (unAcceptableText.contains(String.valueOf(source.charAt(i)))) {
                        return "";
                    }
                }
                return null;
            }
        };
        // 필터 적용: 엔터 입력 제한 및 최대 길이 20자 제한
        inputEditText.setFilters(new InputFilter[]{
                filter1,
                new InputFilter.LengthFilter(20)
        });
        return inputEditText;
    }

    // Fragment 이동
    private void transferTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    // back key 입력 (뒤로 가기 버튼)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // keyCode 로 받는 것이 사실상 back 버튼 밖에 없다고 가정
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - initTime > 3000) {
                Toast.makeText(HomeActivity.this, "종료하려면 한 번 더 누르세요.", Toast.LENGTH_SHORT).show();
                // 현재 시간 저장
                initTime = System.currentTimeMillis();
            } else {
                // Activity 종료
                finish();
            }
        }
        return true;
    }
    /*private void setLoggedOutState(boolean isLoggedOut) {
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedOut", isLoggedOut);
        editor.apply();
    }*/
}
