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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ssutudy.databinding.CsListBinding;
import com.example.ssutudy.databinding.FragmentClassBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ClassFragment extends Fragment {

    private SharedPreferences sharedPref;
    private SharedPreferences sharedPref2;

    private List<CourseItem> classList;
    private ClassAdapter classAdapter;
    private FragmentClassBinding binding;
    private FirebaseFirestore db;
    private AlertDialog inputDialog;

    // 수업 정보를 담는 클래스
    private static class CourseItem {
        public boolean isSelected;
        String documentId; // Firestore 문서 ID
        String CID;        // CID 필드
        String courseName;
        String startDate;
        String studentName;
        String initializationPerLesson;
        String tuition;
        String studentAge;
        String studentSchool;
        String parent;
        String memo;

        CourseItem(String documentId, String CID, String courseName, String startDate, String studentName,
                   String initializationPerLesson, String tuition, String studentAge,
                   String studentSchool, String parent, String memo) {
            this.documentId = documentId;
            this.CID = CID;
            this.courseName = courseName;
            this.startDate = startDate;
            this.studentName = studentName;
            this.initializationPerLesson = initializationPerLesson;
            this.tuition = tuition;
            this.studentAge = studentAge;
            this.studentSchool = studentSchool;
            this.parent = parent;
            this.memo = memo;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentClassBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();
        sharedPref = requireActivity().getSharedPreferences("HomeActivity", Context.MODE_PRIVATE); // userEmail 가져오기
        sharedPref2 = requireActivity().getSharedPreferences("ClassFragment", Context.MODE_PRIVATE); // documentId 가져오기

        classList = new ArrayList<>();
        classAdapter = new ClassAdapter(classList);
        binding.csRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.csRecyclerView.setAdapter(classAdapter);

        // Firestore에서 데이터 가져오기
        fetchClasses();

        // 수업 추가 버튼 클릭 리스너 설정
        binding.csAdd.setOnClickListener(v -> showAddClassDialog());
    }

    private void fetchClasses() {
        String currentUserEmail = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"

        db.collection("Classes")
                .whereEqualTo("UserEmail", currentUserEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    classList.clear();
                    for (DocumentSnapshot document : querySnapshot) {
                        Map<String, Object> data = document.getData();
                        if (data != null) {
                            String documentId = document.getId(); // 문서 ID 가져오기
                            String CID = String.valueOf(data.getOrDefault("CID", "0"));
                            String courseName = (String) data.getOrDefault("ClassName", "");
                            String startDate = (String) data.getOrDefault("StartDate", "");
                            String studentName = (String) data.getOrDefault("StudentName", "");
                            String initializationPerLesson = String.valueOf(data.getOrDefault("ResetCount", "0"));
                            String tuition = String.valueOf(data.getOrDefault("Tuition", "0"));
                            String studentAge = String.valueOf(data.getOrDefault("StudentAge", "0"));
                            String studentSchool = (String) data.getOrDefault("SchoolName", "");
                            String parent = (String) data.getOrDefault("ParentName", "");
                            String memo = (String) data.getOrDefault("Memo", "");

                            CourseItem item = new CourseItem(
                                    documentId,
                                    CID,
                                    courseName,
                                    startDate,
                                    studentName,
                                    initializationPerLesson,
                                    tuition,
                                    studentAge,
                                    studentSchool,
                                    parent,
                                    memo
                            );
                            classList.add(0, item);
                        }
                    }
                    // 정순 정렬
                    classList.sort((item1, item2) -> {
                        int cid1 = Integer.parseInt(item1.CID);
                        int cid2 = Integer.parseInt(item2.CID);
                        return Integer.compare(cid1, cid2);
                    });
                    Collections.reverse(classList); // 정렬된 리스트를 뒤집음

                    classAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch classes", e);
                    Toast.makeText(requireContext(), "수업 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddClassDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.cs_fill_class, null);

        final EditText classNameEditText = dialogView.findViewById(R.id.className);
        final EditText startDateEditText = dialogView.findViewById(R.id.startDDay);
        final EditText studentNameEditText = dialogView.findViewById(R.id.studentName);
        final EditText resetCountEditText = dialogView.findViewById(R.id.initialPerLesson);
        final EditText tuitionEditText = dialogView.findViewById(R.id.tuitionWon);
        final EditText studentAgeEditText = dialogView.findViewById(R.id.studentAge);
        final EditText schoolNameEditText = dialogView.findViewById(R.id.studentSchoolName);
        final EditText parentNameEditText = dialogView.findViewById(R.id.parentName);

        // 입력 필터 정의
        InputFilter noEnterFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String unAcceptableText = "\n"; // 엔터키 제한
                for (int i = start; i < end; i++) {
                    if (unAcceptableText.contains(String.valueOf(source.charAt(i)))) {
                        return ""; // 허용하지 않는 문자일 경우 공백 반환
                    }
                }
                return null; // 허용 가능하면 그대로 반환
            }
        };

        InputFilter numericFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.toString().matches("[0-9]*")) {
                    return null; // 숫자만 허용
                }
                return ""; // 숫자가 아닌 경우 입력 거부
            }
        };
        // 날짜 입력 필터
        InputFilter dateFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String acceptableText = "0123456789-"; // 숫자와 하이픈만 허용
                for (int i = start; i < end; i++) {
                    if (!acceptableText.contains(String.valueOf(source.charAt(i)))) {
                        return ""; // 허용되지 않은 문자 입력 거부
                    }
                }
                return null; // 허용 가능하면 그대로 반환
            }
        };

        InputFilter lengthFilter30 = new InputFilter.LengthFilter(30); // 최대 30자 제한
        InputFilter lengthFilter10 = new InputFilter.LengthFilter(10); // 최대 10자 제한

        // EditText별 필터 설정
        classNameEditText.setFilters(new InputFilter[]{noEnterFilter, lengthFilter30});
        startDateEditText.setFilters(new InputFilter[]{
                dateFilter,
                new InputFilter.LengthFilter(10) // 최대 8자 제한 (YYYY-MM-DD 형식)
        });
        studentNameEditText.setFilters(new InputFilter[]{noEnterFilter, lengthFilter30});
        resetCountEditText.setFilters(new InputFilter[]{numericFilter, lengthFilter10});
        tuitionEditText.setFilters(new InputFilter[]{numericFilter, lengthFilter10});
        studentAgeEditText.setFilters(new InputFilter[]{numericFilter, lengthFilter10});
        schoolNameEditText.setFilters(new InputFilter[]{noEnterFilter, lengthFilter30});
        parentNameEditText.setFilters(new InputFilter[]{noEnterFilter, lengthFilter30});

        inputDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("확인", (dialog, which) -> {
                    String courseName = classNameEditText.getText().toString();
                    String startDate = startDateEditText.getText().toString();
                    String studentName = studentNameEditText.getText().toString();
                    String resetCount = resetCountEditText.getText().toString();
                    String tuition = tuitionEditText.getText().toString();
                    String studentAge = studentAgeEditText.getText().toString();
                    String schoolName = schoolNameEditText.getText().toString();
                    String parentName = parentNameEditText.getText().toString();

                    if (courseName.isEmpty() || startDate.isEmpty() || studentName.isEmpty()) {
                        Toast.makeText(requireContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // CID 생성: 문서 개수를 조사하여 CID 설정
                    getMaxCID().addOnSuccessListener(maxCID -> {
                        // 최대 CID 값 + 1로 새로운 CID 생성
                        int newCID = Integer.parseInt(maxCID) + 1;
                        Log.d("JH", "CID : " + newCID);

                        // ResetCount 값을 기반으로 빈 배열 생성
                        int resetCountValue = resetCount.isEmpty() ? 0 : Integer.parseInt(resetCount);
                        List<String> classDates = new ArrayList<>();

                        // ResetCount 만큼의 초기값 추가 (예: 빈 문자열로 초기화)
                        for (int i = 0; i < resetCountValue; i++) {
                            classDates.add(""); // 초기값 설정 (필요 시 다른 값으로 변경 가능)
                        }
                        // classStartEndTimes 초기화
                        List<Map<String, String>> classStartEndTimes = new ArrayList<>();
                        for (int i = 0; i < resetCountValue; i++) {
                            Map<String, String> timeMap = new HashMap<>();
                            timeMap.put("start", ""); // 시작 시간
                            timeMap.put("end", "");  // 끝 시간
                            classStartEndTimes.add(timeMap);
                        }
                        // classBatchRule 초기화
                        List<Map<String, String>> classBatchRule = new ArrayList<>();

                        // 새로운 수업 데이터를 생성
                        Map<String, Object> newClass = new HashMap<>();
                        newClass.put("CID", newCID);
                        newClass.put("ClassName", courseName);
                        newClass.put("StartDate", startDate);
                        newClass.put("StudentName", studentName);
                        newClass.put("ResetCount", resetCountValue);
                        newClass.put("Tuition", tuition.isEmpty() ? 0 : Double.parseDouble(tuition));
                        newClass.put("StudentAge", studentAge.isEmpty() ? 0 : Integer.parseInt(studentAge));
                        newClass.put("SchoolName", schoolName);
                        newClass.put("ParentName", parentName);
                        // 메모 입력할 때 쓰일 저장 장소
                        newClass.put("Memo", "");
                        // 수업 날짜 배열
                        newClass.put("classDates", classDates); // 수업 날짜 배열 추가
                        // 그 수업에 해당하는 시작 시간과 끝 시간
                        newClass.put("classStartEndTimes", classStartEndTimes);
                        // 일괄 생성 규칙
                        newClass.put("classBatchRule", classBatchRule);


                        // 로그인 한 이메일도 같이 저장
                        SharedPreferences sharedPref = getActivity().getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
                        String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
                        newClass.put("UserEmail", user_email);

                        // Firestore에 새 수업 데이터 추가
                        db.collection("Classes")
                                .add(newClass)
                                .addOnSuccessListener(documentReference -> {

                                    // 모든 선택 상태를 초기화
                                    for (CourseItem item : classList) {
                                        item.isSelected = false;
                                    }

                                    // 새로 추가된 수업 데이터를 CourseItem으로 생성
                                    CourseItem newItem = new CourseItem(
                                            documentReference.getId(),
                                            String.valueOf(newCID),
                                            courseName,
                                            startDate,
                                            studentName,
                                            resetCount.isEmpty() ? "0" : resetCount,
                                            tuition.isEmpty() ? "0" : tuition,
                                            studentAge.isEmpty() ? "0" : studentAge,
                                            schoolName,
                                            parentName,
                                            ""
                                    );
                                    newItem.isSelected = false; // 선택 상태 초기화
                                    classList.add(0, newItem);
                                    classAdapter.notifyDataSetChanged(); // RecyclerView 업데이트
                                    Toast.makeText(requireContext(), "수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Failed to add class", e);
                                    Toast.makeText(requireContext(), "수업 추가 실패", Toast.LENGTH_SHORT).show();
                                });
                    }).addOnFailureListener(e -> {
                        Log.e("Firestore", "CID 생성 실패", e);
                        Toast.makeText(requireContext(), "CID 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("취소", null)
                .create();
        inputDialog.show();
    }

    private class ClassAdapter extends RecyclerView.Adapter<CourseViewHolder> {
        private final List<CourseItem> courseList;

        ClassAdapter(List<CourseItem> courseList) {
            this.courseList = courseList;
        }

        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CsListBinding binding = CsListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CourseViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            CourseItem courseItem = courseList.get(position);
            holder.bind(courseItem);
            holder.binding.classChooseOne.setSelected(courseItem.isSelected);
        }

        @Override
        public int getItemCount() {
            return courseList.size();
        }
    }

    private class CourseViewHolder extends RecyclerView.ViewHolder {
        private final CsListBinding binding;

        CourseViewHolder(CsListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CourseItem item) {
            binding.courseName.setText(item.courseName);
            binding.startDate.setText(item.startDate);
            binding.studentName.setText(item.studentName);

            // 선택 상태 설정
            binding.classChooseOne.setOnClickListener(view -> {
                // 선택된 documentId를 SharedPreferences에 저장
                SharedPreferences.Editor editor = sharedPref2.edit();
                editor.putString("documentId", item.documentId); // 현재 선택한 documentId 저장
                editor.apply();
                // CID를 쓰는 것은 값들 중에 중복이 발생하기 전에 무조건 클릭하게 된다.
                // 그럼 클릭하게 되었을 때, 중복이 문제가 되고, CID값이 의미가 크게 없다면 다른 값을 줘도 되지 않은가?

                // 여기서 중복된 CID 값을 바꾸는 로직.

                // 모든 항목의 선택 상태를 관리
                for (CourseItem otherItem : classList) {
                    otherItem.isSelected = otherItem.CID.equals(item.CID);
                }

                // RecyclerView 갱신
                classAdapter.notifyDataSetChanged();
                Toast.makeText(requireContext(), "현재 선택된 CID: " + item.CID, Toast.LENGTH_SHORT).show();
            });

            // 수업 상세보기 버튼 클릭 리스너 복구
            binding.lookDetails.setOnClickListener(v -> {
                // 자세히 보기 다이얼로그 레이아웃 설정
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View detailView = inflater.inflate(R.layout.cs_look_details, null);

                // 다이얼로그 내 TextView에 데이터 설정
                TextView classNameText = detailView.findViewById(R.id.className);
                TextView startDateText = detailView.findViewById(R.id.startDDay);
                TextView studentNameText = detailView.findViewById(R.id.studentName);
                TextView resetCountText = detailView.findViewById(R.id.initialPerLesson);
                TextView tuitionText = detailView.findViewById(R.id.tuitionWon);
                TextView studentAgeText = detailView.findViewById(R.id.studentAge);
                TextView studentSchoolText = detailView.findViewById(R.id.studentSchool);
                TextView parentNameText = detailView.findViewById(R.id.parentName);

                // 데이터 채우기
                classNameText.setText(item.courseName);
                startDateText.setText("시작일: " + item.startDate);
                resetCountText.setText("초기화 횟수: " + item.initializationPerLesson + "회");
                tuitionText.setText("수업료: " + item.tuition + " 만(원)");
                studentNameText.setText("학생: " + item.studentName);
                studentAgeText.setText("학생 나이: " + item.studentAge + "세");
                studentSchoolText.setText("학교: " + item.studentSchool);
                // 학부모는 null 허용
                if (!(Objects.equals(item.parent, ""))) {
                    parentNameText.setText("학부모: " + item.parent);
                } else {
                    parentNameText.setText("");
                }

                // 다이얼로그 표시
                AlertDialog detailDialog = new AlertDialog.Builder(requireContext())
                        .setView(detailView)
                        .setPositiveButton("확인", null) // 닫기 버튼
                        .create();
                detailDialog.show();
            });

            // 수업 삭제 (휴지통) 클릭 리스너
            binding.csDelete.setOnClickListener(v -> {
                AlertDialog deleteCheckDialog = new AlertDialog.Builder(getContext())
                        .setMessage(item.studentName + " 학생의 수업을 삭제하시겠습니까?")
                        .setPositiveButton("확인", (dialog, which) -> {
                            // Firestore에서 문서 삭제
                            db.collection("Classes").document(item.documentId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        classList.remove(getAdapterPosition());
                                        classAdapter.notifyDataSetChanged();
                                        Toast.makeText(requireContext(), "수업이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Failed to delete class", e);
                                        Toast.makeText(requireContext(), "수업 삭제 실패", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("취소", null)
                        .create();
                deleteCheckDialog.show();
            });

            // 메모 추가 버튼
            binding.memo.setOnClickListener(view -> {
                View memoView = LayoutInflater.from(getContext()).inflate(R.layout.cs_memozi, null);

                // 메모 창 UI 연결
                TextView modifiedDateView = memoView.findViewById(R.id.modifiedDate);
                TextView memoContentView = memoView.findViewById(R.id.memoContent);
                EditText addMemoEditText = memoView.findViewById(R.id.addMemo);

                // 수정 날짜와 기존 메모 설정
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String currentDate = dateFormat.format(new Date());
                modifiedDateView.setText(currentDate);

                // Firebase에서 메모 데이터 가져오기
                db.collection("Classes").document(item.documentId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Firestore에서 Memo 필드 가져오기
                                String existingMemo = documentSnapshot.getString("Memo");
                                if (existingMemo != null) {
                                    memoContentView.setText(existingMemo); // 가져온 메모를 TextView에 설정
                                } else {
                                    memoContentView.setText(""); // 메모가 없으면 빈 문자열로 설정
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Failed to fetch memo", e);
                            Toast.makeText(requireContext(), "메모를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        });

                AlertDialog memoDialog = new AlertDialog.Builder(requireContext())
                        .setView(memoView)
                        .setPositiveButton("확인", (dialog, which) -> {
                            // 입력된 메모 가져오기
                            String addMemo = addMemoEditText.getText().toString();

                            // 현재 날짜 포맷
                            String currentDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());


                            // Firestore에서 기존 메모 가져오기
                            db.collection("Classes").document(item.documentId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            // 기존 메모 가져오기
                                            String currentMemo = documentSnapshot.getString("Memo");

                                            // 기존 메모와 새 메모 병합
                                            String updatedMemo = (currentMemo == null || currentMemo.isEmpty())
                                                    ? currentDay + "\n    " + addMemo + "\n"
                                                    : currentMemo + "\n" + currentDay + "\n    " + addMemo + "\n";

                                            if (!(addMemo.equals(""))) {
                                                // Firestore에 업데이트
                                                db.collection("Classes").document(item.documentId)
                                                        .update("Memo", "\n" + updatedMemo)
                                                        .addOnSuccessListener(aVoid -> {
                                                            // 로컬 객체의 메모도 업데이트
                                                            item.memo = updatedMemo;
                                                            Toast.makeText(requireContext(), "메모가 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(requireContext(), "메모 추가 실패", Toast.LENGTH_SHORT).show();
                                                        });
                                            }
                                        } else {
                                            Toast.makeText(requireContext(), "메모를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Firestore에서 메모 읽기 실패", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        // 메모 삭제 버튼 추가
                        .setNeutralButton("삭제", (dialog, which) -> {
                            // 메모 삭제 확인 대화 상자
                            AlertDialog deleteCheckDialog = new AlertDialog.Builder(requireContext())
                                    .setMessage("메모를 삭제하시겠습니까?")
                                    .setPositiveButton("확인", (deleteDialog, deleteWhich) -> {
                                        // Firestore에서 메모 필드를 빈 문자열로 업데이트
                                        db.collection("Classes").document(item.documentId)
                                                .update("Memo", "")
                                                .addOnSuccessListener(aVoid -> {
                                                    item.memo = ""; // 로컬 객체에서 메모 초기화
                                                    Toast.makeText(requireContext(), "메모가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("Firestore", "Failed to delete memo", e);
                                                    Toast.makeText(requireContext(), "메모 삭제 실패", Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .setNegativeButton("취소", null) // 취소 버튼
                                    .create();

                            deleteCheckDialog.show();
                        })
                        .setNegativeButton("취소", null)
                        .create();

                memoDialog.show();
            });
        }
    }

    // Classes에서 존재하는 문서들의 CID 중에서 가장 큰 값 리턴하는 함수
    public Task<String> getMaxCID() {
        // 로그인 한 이메일에 해당하는 수업들 중에서 CID 최대값 조사
        SharedPreferences sharedPref = getActivity().getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
        String user_email = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
        Log.d("JH", "getMaxCID's userEmail : "+user_email);

        return db.collection("Classes")
                .whereEqualTo("UserEmail", user_email)
                .orderBy("CID", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Log.d("JH", "Task successful");
                        if (task.getResult() != null) {
                            Log.d("JH", "Query result size: " + task.getResult().size());
                        } else {
                            Log.d("JH", "Query result is null");
                        }
                    } else {
                        Log.d("JH", "Task failed", task.getException());
                    }
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        int maxCID = task.getResult().getDocuments().get(0).getLong("CID").intValue();
                        Log.d("JH", "maxCID: " + maxCID);
                        return String.valueOf(maxCID);
                    } else {
                        return "0";
                    }
                });
    }
}
