package com.example.ssutudy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ssutudy.databinding.FragmentHWBinding;
import com.example.ssutudy.databinding.HwListBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HWFragment extends Fragment {

    private int N; // 숙제 개수
    private FragmentHWBinding binding;
    private FirebaseFirestore db; // Firestore 인스턴스
    private String documentId; // ClassFragment에서 선택한 문서 ID
    private List<HomeworkItem> list; // 숙제 리스트
    private MyAdapter myAdapter; // RecyclerView 어댑터
    private AlertDialog inputDialog;

    private static class HomeworkItem {
        String round; // 회차
        String textbookName; // 교재 이름
        String pageRange; // 페이지 범위
        String iconType; // "good", "bad", "none"
        boolean isChecked; // 체크 박스 상태

        HomeworkItem(String round, String textbookName, String pageRange) {
            this.round = round;
            this.textbookName = textbookName;
            this.pageRange = pageRange;
            this.iconType = "none";
            this.isChecked = false;
        }
        HomeworkItem(String round, String textbookName, String pageRange, String iconType, boolean isChecked) {
            this.round = round;
            this.textbookName = textbookName;
            this.pageRange = pageRange;
            this.iconType = iconType;
            this.isChecked = isChecked;
        }
    }

    public HWFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHWBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        // SharedPreferences에서 documentId 가져오기
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("ClassFragment", Context.MODE_PRIVATE);
        documentId = sharedPref.getString("documentId", null);

        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(requireContext(), "수업을 먼저 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 초기 회차 수 설정
        N = 0;

        // 숙제 리스트 초기화
        list = new ArrayList<>();

        // RecyclerView 설정
        myAdapter = new MyAdapter(list);
        binding.HWrecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.HWrecyclerView.setAdapter(myAdapter);

        // Firestore에서 숙제 데이터 가져오기
        fetchHomeworkData();
        Log.d("JH", "HW's now : " + documentId);

        // + 버튼 리스너 설정
        binding.HWplus.setOnClickListener(v -> showAddHomeworkDialog());

        // 휴지통 버튼 리스너 설정
        binding.HWdelete.setOnClickListener(v -> deleteLastHomework());
    }

    private void fetchHomeworkData() {
        db.collection("Classes").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        list.clear();
                        N = 0;

                        List<String> keys = new ArrayList<>(data.keySet());
                        //keys.sort((key1, key2) -> key2.compareTo(key1)); // Homework1, Homework2, Homework3 순으로 정렬
                        keys.sort(String::compareTo); // 역순 정렬 (Homework6, Homework5, Homework4 순)
                        for (String key : keys) {
                            // { homework* } 요소들 가져오기
                            if (key.startsWith("homework")) {
                                //@SuppressWarnings("unchecked")

                                Map<String, Object> homework = (Map<String, Object>) data.get(key);
                                String round = (String) homework.get("round");
                                String textbookName = (String) homework.get("textbookName");
                                String pageRange = (String) homework.get("pageRange");
                                String iconType = (String) homework.get("iconType");
                                String isChecked = (String) homework.get("isChecked");
                                boolean isCheck;
                                if (isChecked.equals("true")) {
                                    isCheck = true;
                                } else if (isChecked.equals("false")) {
                                    isCheck = false;
                                } else {
                                    isCheck = false;
                                }

                                // 리스트에 넣을 HomeworkItem 객체 생성
                                HomeworkItem hwItem = new HomeworkItem(round + "회차", "    교재 이름 : " + textbookName,
                                        "    페이지 범위 : " + pageRange + "p", iconType, isCheck);


                                // 실제로 recyclerView 에 보일 놈들
                                list.add(0, hwItem);

                                N++;
                            }
                        }

                        // RecyclerView 업데이트
                        myAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(requireContext(), "수업 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HWFragment", "Failed to fetch Homework data", e);
                    Toast.makeText(requireContext(), "숙제를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddHomeworkDialog() {
        LayoutInflater dialogInflater = LayoutInflater.from(getContext());
        View dialogView = dialogInflater.inflate(R.layout.hw_fill_homework, null);

        final EditText textbookNameEditText = dialogView.findViewById(R.id.textbookName);
        final EditText pageRangeEditText = dialogView.findViewById(R.id.pageRange);

        // 교재 입력 받을 때 enter 입력 받지 못하게 제한 걸기
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
        // 페이지 범위 입력 받는 거 제한 걸기
        InputFilter filter2 = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String acceptableText = "0123456789~-, ";

                for (int i = start; i < end; i++) {
                    if (!acceptableText.contains(String.valueOf(source.charAt(i)))) {
                        return "";
                    }
                }
                return null;
            }
        };
        textbookNameEditText.setFilters(new InputFilter[]{
                filter1,
                new InputFilter.LengthFilter(30)
        });
        pageRangeEditText.setFilters(new InputFilter[]{
                filter2,
                new InputFilter.LengthFilter(30)
        });


        inputDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("추가", (dialog, which) -> {
                    String textbookName = textbookNameEditText.getText().toString();
                    String pageRange = pageRangeEditText.getText().toString();

                    // 모두 입력 받도록
                    if (textbookName.isEmpty() || pageRange.isEmpty()) {
                        Toast.makeText(requireContext(), "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 입력 받은 값들 documentId의 문서에 올리기
                    String homeworkKey = "homework" + (N + 1); // 새로 추가할 키
                    Map<String, Object> newHomework = new HashMap<>();
                    newHomework.put("round", String.valueOf(N + 1));
                    newHomework.put("textbookName", textbookName);
                    newHomework.put("pageRange", pageRange);

                    newHomework.put("iconType", "none"); // 이미지
                    newHomework.put("isChecked", "false"); // 보이는가?

                    db.collection("Classes").document(documentId)
                            .update(homeworkKey, newHomework)
                            .addOnSuccessListener(aVoid -> {
                                // 앞에다 저장
                                list.add(0, new HomeworkItem(String.valueOf(N + 1) + "회차",
                                        "    페이지 범위 : " + textbookName, "    교재 이름 : " + pageRange + "p"));
                                N++;
                                myAdapter.notifyDataSetChanged();
                                Toast.makeText(requireContext(), "숙제가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("HWFragment", "Failed to add Homework", e);
                                Toast.makeText(requireContext(), "숙제 추가 실패", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("취소", null)
                .create();
        inputDialog.show();
    }

    private void deleteLastHomework() {
        if (N > 0) {
            String lastHomeworkKey = "homework" + N;

            db.collection("Classes").document(documentId)
                    .update(lastHomeworkKey, FieldValue.delete()) // map 에 존재하는 값들이 모두 null이면 그 map<>을 삭제한다.
                    .addOnSuccessListener(aVoid -> {
                        // 리스트 지우기
                        list.remove(0);
                        N--;
                        myAdapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), "숙제가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HWFragment", "Failed to delete Homework", e);
                        Toast.makeText(requireContext(), "숙제 삭제 실패", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(requireContext(), "삭제할 숙제가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        private final HwListBinding binding;

        private MyViewHolder(HwListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(HomeworkItem item) {
            binding.hwListView.setText(item.round);
            binding.textbookName.setText(item.textbookName);
            binding.pageRange.setText(item.pageRange);

            // ImageView visibility 설정
            int visibility = View.INVISIBLE; // View.VISIBLE, View.INVISIBLE, View.GONE
            if (item.isChecked) {
                visibility = View.VISIBLE;
            } else if (!item.isChecked) {
                visibility = View.INVISIBLE;
            }
            binding.HWprogress.setVisibility(visibility);
            // ImageType 에 따른 src 설정 "good", "bad", "none"
            setImageByType(binding.HWprogress, item.iconType);

            // 숙제 완성도 체크
            binding.hwCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
                    View dialogView2 = inflater.inflate(R.layout.hw_check_good_bad, null);

                    AlertDialog chooseDialog = new AlertDialog.Builder(binding.getRoot().getContext())
                            .setView(dialogView2)
                            .create();
                    chooseDialog.show();

                    dialogView2.findViewById(R.id.goodButton).setOnClickListener(view -> {
                        Log.d("JH", item.round.charAt(0) + "'s HW is good !!!");
                        item.iconType = "good";
                        binding.HWprogress.setImageResource(R.drawable.baseline_mood_24);
                        binding.HWprogress.setVisibility(View.VISIBLE);

                        // Firebase 업데이트 호출
                        updateFirebase(item, "good");

                        chooseDialog.dismiss();
                    });

                    dialogView2.findViewById(R.id.badButton).setOnClickListener(view -> {
                        Log.d("JH", item.round.charAt(0) + "'s HW is bad.. OTL");
                        item.iconType = "bad";
                        binding.HWprogress.setImageResource(R.drawable.baseline_mood_bad_24);
                        binding.HWprogress.setVisibility(View.VISIBLE);

                        // Firebase 업데이트 호출
                        updateFirebase(item, "bad");

                        chooseDialog.dismiss();
                    });
                } else {
                    item.iconType = "none";
                    binding.HWprogress.setVisibility(View.INVISIBLE);

                    // Firebase 업데이트 호출
                    updateFirebase(item, "none");
                }
            });

            // 숙제 알림 보내기
            binding.HWnotification.setOnClickListener(view -> Log.d("JH", item.round.charAt(0) + "'s notification start~"));
        }

        // 클릭 시 Firebase 업데이트 메서드 추가
        private void updateFirebase(HomeworkItem item, String iconType) {
            // Firestore 인스턴스 가져오기
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Firestore 경로 설정
            String DocumentId = documentId; // HWFragment.java 에 문서 ID가 있다고 가정
            String homeworkId = "homework" + item.round.charAt(0); // homework1, homework2 등

            // Firestore에서 기존 데이터 가져오기
            db.collection("Classes")
                    .document(DocumentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // 현재 데이터 가져오기
                            Map<String, Object> homeworkData = (Map<String, Object>) documentSnapshot.get(homeworkId);

                            if (homeworkData != null) {
                                // 기존 데이터 수정
                                homeworkData.put("iconType", iconType);
                                homeworkData.put("isChecked", "true");

                                // Firestore에 업데이트
                                db.collection("Classes")
                                        .document(documentId)
                                        .update(homeworkId, homeworkData) // homework1 필드 업데이트
                                        .addOnSuccessListener(aVoid -> Log.d("JH", "Successfully updated homework: " + homeworkId))
                                        .addOnFailureListener(e -> Log.e("FirestoreUpdate", "Failed to update homework: " + homeworkId, e));
                            } else {
                                Log.e("FirestoreUpdate", "Homework data not found: " + homeworkId);
                                Toast.makeText(requireContext(), "해당 숙제를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("FirestoreUpdate", "Document not found: " + documentId);
                            Toast.makeText(requireContext(), "문서를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Failed to fetch document", e);
                        Toast.makeText(requireContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    });
        }
        private void setImageByType(ImageView imageView, String imageType) {
            switch (imageType) {
                case "good":
                    imageView.setImageResource(R.drawable.baseline_mood_24); // 긍정적인 상태 이미지
                    imageView.setVisibility(View.VISIBLE); // 이미지를 보이게 설정
                    break;

                case "bad":
                    imageView.setImageResource(R.drawable.baseline_mood_bad_24); // 부정적인 상태 이미지
                    imageView.setVisibility(View.VISIBLE); // 이미지를 보이게 설정
                    break;
                default:
                    imageView.setVisibility(View.INVISIBLE); // 기본 상태에서는 이미지 숨기기
                    break;
            }
        }
    }
    
    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private final List<HomeworkItem> list;

        private MyAdapter(List<HomeworkItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            HwListBinding binding = HwListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MyViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            HomeworkItem item = list.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
