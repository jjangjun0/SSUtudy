package com.example.ssutudy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ssutudy.databinding.ActivityHelpBinding;
import com.example.ssutudy.databinding.HelpItemBinding;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {

    private static class HelpItem {
        String itemSubject;
        String itemFunction;

        HelpItem(String itemSubject, String itemFunction) {
            this.itemSubject = itemSubject;
            this.itemFunction = itemFunction;
        }
    }

    private HelpAdapter helpAdapter;  // RecyclerView Adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelpBinding binding = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<String> strList = getStrings(); // 도움말에 대한 내용 받아오기
        int length = strList.size();

        // 도움말에 들어갈 내용 list에 넣기
        List<HelpItem> helpList = new ArrayList<>();
        for (int i = 0; i < length; i += 2) {
            String str1 = strList.get(i);
            String str2 = (i + 1 < length) ? strList.get(i + 1) : "";

            HelpItem newHelp = new HelpItem(str1, str2);
            helpList.add(newHelp);
        }

        // Recycler view
        helpAdapter = new HelpAdapter(helpList);
        binding.HelpRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.HelpRecyclerView.setAdapter(helpAdapter);

        // 뒤로가기 버튼
        binding.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HelpActivity.this, HomeActivity.class);
                // 새로운 인스턴스를 생성하지 않고, 기존의 HomeActivity 를 스택에서 가져온다.
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });
    }

    private static @NonNull List<String> getStrings() {
        List<String> strList = new ArrayList<>();
        {
            // 도움말에 작성할 문장들 넣기
            strList.add("[구성]");
            strList.add(
                    "기본 화면\n" +
                            "    - 툴바\n" +
                            "            - 사용자 이름 변경\n" +
                            "            - 비밀번호 변경\n" +
                            "            - 과외 시작일 지정\n" +
                            "            - 로그아웃\n" +
                            "            - 도움말 보기\n" +
                            "\n" +
                            "    - 하단 내비게이션\n" +
                            "            - 홈 화면\n" +
                            "            - 캘린더 화면\n" +
                            "            - 수업 화면\n" +
                            "            - 숙제 화면\n" +
                            "            - 채팅 화면"
            );
            strList.add("[화면 기능]");
            strList.add(
                    "1. 홈 화면\n" +
                            "    : 지금까지 며칠 과외를 해왔는지 알 수 있다.\n" +
                            "\n" +
                            "2. 캘린더 화면\n" +
                            "    : 일정을 생성, 취소를 할 수 있다.\n" +
                            "\n" +
                            "3. 수업 화면\n" +
                            "    : 수업의 목록을 토대로 수업에 대한 세부사항을 파악할 수 있다.\n" +
                            "\n" +
                            "4. 숙제 화면\n" +
                            "    : 숙제를 추가, 삭제를 할 수 있다. 각 회차의 숙제마다 숙제 완료도를 체크할 수 있다. 학생에게 숙제 알림을 보낼 수 있다.\n" +
                            "\n" +
                            "5. 채팅 화면\n" +
                            "    : 학생과 선생님이 채팅할 수 있다."
            );
            strList.add("[부가 기능]");
            strList.add(
                    "1. 자동 로그인\n" +
                            "    : 한 번 로그인 하면 다음 부팅 시, 전에 로그인 했던 계정으로 자동 로그인 됩니다.\n" +
                            "\n" +
                            "2. 로그아웃\n" +
                            "    : 로그인 되어 있는 이메일과 사용자 이름, 그리고 과외 시작일이 초기화 됩니다.\n" +
                            "\n" +
                            "3. 과외 시작일 설정\n" +
                            "    : 과외 시작일이 오늘 기준으로 미래라면 '0'으로 입력되어 있다가, 시간이 지나고 과외 시작일부터 카운트 됩니다.\n" +
                            "\n" +
                            "4. 앱 종료\n" +
                            "    : 홈 화면에서 뒤로 가기를 연속으로 3초 이내에 누르세요."
            );
        }
        return strList;
    }

    // ViewHolder for the Help Items
    private class HelpViewHolder extends RecyclerView.ViewHolder {
        private HelpItemBinding binding;

        public HelpViewHolder(HelpItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HelpItem item) {
            binding.itemSubject.setText(item.itemSubject);
            binding.itemContent.setText(item.itemFunction);
        }
    }

    // Adapter for the RecyclerView
    private class HelpAdapter extends RecyclerView.Adapter<HelpViewHolder> {
        private List<HelpItem> helpItems;

        public HelpAdapter(List<HelpItem> helpItems) {
            this.helpItems = helpItems;
        }

        @NonNull
        @Override
        public HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            HelpItemBinding binding = HelpItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new HelpViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull HelpViewHolder holder, int position) {
            HelpItem helpItem = helpItems.get(position);
            holder.bind(helpItem);
        }

        @Override
        public int getItemCount() {
            return helpItems.size();
        }
    }
}
