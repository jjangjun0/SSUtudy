package com.example.ssutudy;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.ssutudy.databinding.FragmentCalendarBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CalendarFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public CalendarFragment() {
        // Required empty public constructor
    }

    public static CalendarFragment newInstance(String param1, String param2) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private FragmentCalendarBinding binding;

    SharedPreferences sharedPref;
    SharedPreferences sharedPref2;
    String studentName; // 학생 이름
    int resetCount; // 수업 초기화 횟수
    String toDayStr; // 오늘 날짜
    String documentId; // 문서 이름

    FirebaseFirestore db;
    DocumentReference docRef;

    Thread CalendarPaint; // 캘린더 색칠하기

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot(); // 바인딩된 루트 뷰 반환
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();
        Date toDate = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        toDayStr = dateFormat.format(toDate);

        super.onViewCreated(view, savedInstanceState);

        sharedPref = requireActivity().getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
        sharedPref2 = requireActivity().getSharedPreferences("ClassFragment", Context.MODE_PRIVATE);

        String currentUserEmail = sharedPref.getString("userEmail", "noEmail");
        documentId = sharedPref2.getString("documentId", null);

        db = FirebaseFirestore.getInstance();

        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(requireContext(), "수업을 먼저 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("JH", documentId);
        docRef = db.collection("Classes").document(documentId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        studentName = document.getString("StudentName");
                        resetCount = Objects.requireNonNull(document.getLong("ResetCount")).intValue();

                        Log.d("JH", "학생 이름: " + studentName);
                        binding.calendarStudentName.setText(studentName + " 학생");
                    } else {
                        Log.d("JH", "해당 문서가 존재하지 않습니다");
                    }
                } else {
                    Log.d("JH", "데이터 가져오기 실패: ", task.getException());
                }
            }
        });
        // 파이어베이스에서 classDates 배열 가지고 와서 WantColorDecorator() 타일 색칠 하기
        CalendarPaint = new Thread(() -> {
            // Firestore에서 데이터 가져오기
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> classDates = (List<String>) documentSnapshot.get("classDates");
                    for (String dateString : classDates) {
                        Log.d("JH", dateString);
                    }
                    if (!classDates.isEmpty()) {
                        List<CalendarDay> calendarDays = new ArrayList<>();

                        // Firestore에서 가져온 날짜 문자열을 CalendarDay 객체로 변환
                        for (String dateString : classDates) {
                            if (Objects.equals(dateString, "")) {
                                continue;
                            }
                            String[] parts = dateString.split("-"); // 날짜 형식: YYYY-MM-DD
                            int year = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int day = Integer.parseInt(parts[2]);
                            //Log.d("JH", year + "_" + month + "_" + day);

                            // 그릴 때 지난 날짜는 색깔 표시 안 되도록.
                            if (dateString.compareTo(toDayStr) >= 0) {
                                calendarDays.add(CalendarDay.from(year, month, day));
                            }
                        }

                        // UI 업데이트는 메인 스레드에서 실행해야 함
                        requireActivity().runOnUiThread(() -> {
                            binding.calendarView.addDecorator(new WantColorDecorator(calendarDays, 0x804169E1));
                        });
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "수업 날짜가 없습니다.", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "문서를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    );
                }
            }).addOnFailureListener(e -> {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "수업 날짜를 가져오지 못했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            });
        });
        CalendarPaint.start();

        // 캘린더 일괄 생성 클릭
        binding.calendarModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("JH", toDayStr);
                AlertDialog calendarModify = new AlertDialog.Builder(requireContext())
                        .setTitle("[ 일정 관리 ]")
                        .setMessage("수업 일괄 생성을 하면, 그 전에 지정한 일정은 초기화 됩니다.")
                        .setNeutralButton("수업 일괄 생성", (dialog, which) -> {
                            int[] classPerWeek = new int[1]; // 주에 수업 몇 회 하는지 저장
                            String[] dayOfTheWeek = {""}; // 선택된 요일 저장
                            List<String> classDates = new ArrayList<>(); // 수업 날짜 저장
                            List<Map<String, String>> LocalClassStartEndTimes = new ArrayList<>(); // 시작/끝 시간 저장

                            // 1. 수업을 주 몇 회 하는지 입력받는 다이얼로그
                            AlertDialog.Builder numberDialog = new AlertDialog.Builder(requireContext());
                            numberDialog.setTitle("한 주에 수업 횟수 입력")
                                    .setSingleChoiceItems(new String[]{"1회", "2회", "3회", "4회", "5회", "6회", "7회"}, -1, (dialogInterface, whichOption) -> {
                                        classPerWeek[0] = whichOption + 1;
                                    })
                                    .setPositiveButton("확인", (dialogInterface, whichOption) -> {
                                        if (classPerWeek[0] == 0) {
                                            Toast.makeText(getContext(), "수업 횟수를 선택하세요.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        // 2. 수업 요일 입력받는 다이얼로그로 이동
                                        boolean[] checkedDays = new boolean[7];
                                        String[] days = {"월", "화", "수", "목", "금", "토", "일"};

                                        AlertDialog.Builder dayDialog = new AlertDialog.Builder(requireContext());
                                        dayDialog.setTitle("요일 선택")
                                                .setMultiChoiceItems(days, checkedDays, (dialog1, whichDay, isChecked) -> {
                                                    checkedDays[whichDay] = isChecked;
                                                })
                                                .setPositiveButton("확인", (dialog1, which1) -> {
                                                    // 요일 저장하기
                                                    StringBuilder selectedDays = new StringBuilder();
                                                    int count = 0;
                                                    for (int i = 0; i < checkedDays.length; i++) {
                                                        if (checkedDays[i]) {
                                                            selectedDays.append(days[i]).append("$");
                                                            count++;
                                                        }
                                                    }

                                                    if (count != classPerWeek[0]) {
                                                        Toast.makeText(getContext(), "선택한 요일의 개수가 수업 횟수와 맞지 않습니다.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }

                                                    // 저장 후 다음 단계 진행
                                                    dayOfTheWeek[0] = selectedDays.toString();

                                                    // toDayStr(오늘 날짜)를 기준으로 하나씩 증가시키면서 선택한 요일에 해당하는 날짜를 차례대로 리스트에
                                                    // "YYYY-MM-DD" (String)형태로 저장한다. 그리고, 배열의 크기가 resetCount가 되었다면 반복문을 종료한다.
                                                    Calendar calendar = Calendar.getInstance();
                                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                    String[] splitDays = dayOfTheWeek[0].split("\\$");

                                                    while (classDates.size() < resetCount) {
                                                        String currentDay = new SimpleDateFormat("E", Locale.getDefault()).format(calendar.getTime());
                                                        for (String day : splitDays) {
                                                            if (day.equals(currentDay)) {
                                                                classDates.add(dateFormat.format(calendar.getTime()));
                                                            }
                                                        }
                                                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                                                    }

                                                    // 동적으로 버튼 생성
                                                    LinearLayout buttonLayout = new LinearLayout(requireContext());
                                                    buttonLayout.setOrientation(LinearLayout.VERTICAL);

                                                    for (String day : splitDays) {
                                                        if (day.isEmpty()) continue;

                                                        Button dayButton = new Button(requireContext());
                                                        dayButton.setText(day + "요일");
                                                        dayButton.setOnClickListener(view -> {
                                                            Calendar c = Calendar.getInstance();
                                                            int hour = c.get(Calendar.HOUR_OF_DAY);
                                                            int minute = c.get(Calendar.MINUTE);

                                                            TimePickerDialog startDialog = new TimePickerDialog(requireContext(), (view1, startHourOfDay, startMinute) -> {
                                                                String startTime = startHourOfDay + ":" + startMinute;

                                                                TimePickerDialog endDialog = new TimePickerDialog(requireContext(), (view2, endHourOfDay, endMinute) -> {
                                                                    String endTime = endHourOfDay + ":" + endMinute;

                                                                    // 시작 시간과 종료 시간을 검증
                                                                    if (endHourOfDay < startHourOfDay || (endHourOfDay == startHourOfDay && endMinute < startMinute)) {
                                                                        // 시작 시간이 종료 시간보다 늦으면 빈 문자열 저장
                                                                        Map<String, String> timeMap = new HashMap<>();
                                                                        timeMap.put("start", "");
                                                                        timeMap.put("end", "");
                                                                        timeMap.put("day", day);
                                                                        LocalClassStartEndTimes.add(timeMap);
                                                                        Toast.makeText(getContext(), day + "의 종료 시간이 시작 시간보다 이릅니다. 시간을 다시 설정하세요.", Toast.LENGTH_SHORT).show();
                                                                    } else {
                                                                        // 유효한 시작 시간과 종료 시간 저장
                                                                        Map<String, String> timeMap = new HashMap<>();
                                                                        timeMap.put("start", startTime);
                                                                        timeMap.put("end", endTime);
                                                                        timeMap.put("day", day);
                                                                        LocalClassStartEndTimes.add(timeMap);
                                                                        Toast.makeText(getContext(), day + " 시작: " + startTime + " 종료: " + endTime, Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }, hour, minute, false);
                                                                endDialog.setTitle(day + "요일 종료 시간 설정");
                                                                endDialog.show();
                                                            }, hour, minute, false);
                                                            startDialog.setTitle(day + "요일 시작 시간 설정");
                                                            startDialog.show();
                                                        });

                                                        buttonLayout.addView(dayButton);
                                                    }

                                                    // 다이얼로그로 버튼 레이아웃 표시
                                                    AlertDialog.Builder buttonDialog = new AlertDialog.Builder(requireContext());
                                                    buttonDialog.setTitle("요일별 시간 설정")
                                                            .setView(buttonLayout)
                                                            .setPositiveButton("완료", (dialog2, which2) -> {
                                                                // Firebase 업데이트
                                                                docRef.update("classDates", classDates);
                                                                // 일괄 생성 규칙
                                                                docRef.update("classBatchRule", LocalClassStartEndTimes);

                                                                // savedClassStartEndTimes 생성 및 저장
                                                                List<Map<String, String>> savedClassStartEndTimes = new ArrayList<>();
                                                                for (String date : classDates) {
                                                                    String dayOfWeek = null;
                                                                    try {
                                                                        dayOfWeek = new SimpleDateFormat("E", Locale.getDefault()).format(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));
                                                                    } catch (ParseException e) {
                                                                        throw new RuntimeException(e);
                                                                    }
                                                                    for (Map<String, String> timeMap : LocalClassStartEndTimes) {
                                                                        if (timeMap.get("day").equals(dayOfWeek)) {
                                                                            Map<String, String> savedTimeMap = new HashMap<>();
                                                                            savedTimeMap.put("start", timeMap.get("start"));
                                                                            savedTimeMap.put("end", timeMap.get("end"));
                                                                            savedClassStartEndTimes.add(savedTimeMap);
                                                                            break;
                                                                        }
                                                                    }
                                                                }

                                                                docRef.update("classStartEndTimes", savedClassStartEndTimes);
                                                            })
                                                            .setNegativeButton("취소", null)
                                                            .create().show();
                                                })
                                                .setNegativeButton("취소", null)
                                                .create().show();
                                    })
                                    .setNegativeButton("취소", null)
                                    .create().show();
                        })



                        .setNegativeButton("초기화", (dialog, which) -> {
                            AlertDialog checkDialog = new AlertDialog.Builder(requireContext())
                                    .setMessage("일정을 초기화하시겠습니까?")
                                    .setPositiveButton("확인", (addDialog, addWhich) -> {
                                        docRef.get().addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                // classDates 초기화
                                                List<String> classDates = (List<String>) documentSnapshot.get("classDates");
                                                // 배열 모두 ""로 만든다.
                                                for (int i = 0; i < resetCount; i++) {
                                                    classDates.set(i, "");
                                                }
                                                docRef.update("classDates", classDates)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(getContext(), "일정이 초기화 되었습니다.", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });

                                                // classStartEndTimes 초기화 추가
                                                List<Map<String, String>> classStartEndTimes = (List<Map<String, String>>) documentSnapshot.get("classStartEndTimes");
                                                if (classStartEndTimes != null) {
                                                    for (int i = 0; i < classStartEndTimes.size(); i++) {
                                                        Map<String, String> timeMap = classStartEndTimes.get(i);
                                                        timeMap.put("start", "");
                                                        timeMap.put("end", "");
                                                    }
                                                    docRef.update("classStartEndTimes", classStartEndTimes)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Toast.makeText(getContext(), "시간 초기화가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(getContext(), "시간 초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            });
                                                }
                                                // classBatchRule 초기화 -> null로 설정
                                                List<Map<String, String>> classBatchRule = (List<Map<String, String>>) documentSnapshot.get("classBatchRule");
                                                if (classBatchRule != null) {
                                                    docRef.update("classBatchRule", null);
                                                }
                                            }
                                        });
                                    })
                                    .setNegativeButton("취소", null)
                                    .create();

                            checkDialog.show();
                        })
                        .setPositiveButton("취소", null)
                        .create();
                calendarModify.show();
            }
        });
        // 날짜 두 번 클릭
        binding.calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            private long lastClickTime = 0; // 마지막 클릭 시간 저장

            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 300) { // 300ms 이내에 클릭하면 이중 클릭으로 간주
                    //Toast.makeText(getContext(), "Double-click detected on " + date, Toast.LENGTH_SHORT).show();
                    // 이중 클릭 동작 추가
                    int year = date.getYear();
                    int month = date.getMonth();
                    int day = date.getDay();

                    String formattedDate = String.format("%04d-%02d-%02d", year, month, day);
                    String koreanDay = month + "월 " + day + "일";

                    AlertDialog thisDayCheckDialog = new AlertDialog.Builder(requireContext())
                            .setTitle("[" +koreanDay + " 수업" + "]")
                            .setNeutralButton("생성", (dialog, which) -> {
                                AlertDialog addClassDialog = new AlertDialog.Builder(requireContext())
                                        .setMessage(koreanDay + " 수업을 추가하시겠습니까?")
                                        .setPositiveButton("확인", (addDialog, addWhich) -> {
                                            docRef.get().addOnSuccessListener(documentSnapshot -> {
                                                if (documentSnapshot.exists()) {
                                                    List<String> classDates = (List<String>) documentSnapshot.get("classDates");
                                                    List<Map<String, String>> classStartEndTimes = (List<Map<String, String>>) documentSnapshot.get("classStartEndTimes");
                                                    // 수업 날짜의 개수가 resetCount라면 생성하면 안된다.
                                                    int count = 0;
                                                    boolean okay = true;
                                                    for (String dates : classDates) {
                                                        if (!Objects.equals(dates, "")) {
                                                            count++;
                                                        }
                                                    }
                                                    if (count == resetCount) {
                                                        okay = false;
                                                    }
                                                    // resetCount가 아니라면
                                                    if (okay) {
                                                        if (classDates == null) {
                                                            classDates = new ArrayList<>();
                                                        }

                                                        int insertIndex = 0;
                                                        while (!Objects.equals(classDates.get(insertIndex), "") && insertIndex < classDates.size() && classDates.get(insertIndex).compareTo(formattedDate) < 0) {
                                                            insertIndex++;
                                                        }
                                                        if (!Objects.equals(classDates.get(insertIndex), formattedDate)) { // 같은 날짜가 있으면 무시
                                                            classDates.add(insertIndex, formattedDate);
                                                            classDates.remove(resetCount);

                                                            // 해당 인덱스의 classStartEndTimes 초기화
                                                            if (classStartEndTimes != null && classStartEndTimes.size() > insertIndex) {
                                                                Map<String, String> timeMap = classStartEndTimes.get(insertIndex);
                                                                timeMap.put("start", "");
                                                                timeMap.put("end", "");
                                                            }

                                                            docRef.update("classDates", classDates)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Toast.makeText(getContext(), koreanDay + " 수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "수업 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    });

                                                            docRef.update("classStartEndTimes", classStartEndTimes)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Toast.makeText(getContext(), "시간 정보가 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "시간 초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    });
                                                        }
                                                    } else {
                                                        // resetCount가 넘었는데
                                                        if (!Objects.equals(classDates.get(resetCount - 1), formattedDate)) { // 같은 날짜가 있으면 무시
                                                            // 만약 날짜를 추가할 formattedDate가 classDates.get(resetCount-1)의 날짜보다 크면
                                                            // Toast만 띄울거야. "지정한 수업 cycle 이외 미래의 수업은\n지정할 수 없습니다." 이렇게.
                                                            if (formattedDate.compareTo(classDates.get(resetCount - 1)) > 0) {
                                                                Toast.makeText(getContext(), "지정한 수업 cycle 이외 미래의 수업은\n지정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                                            } else { // 미래가 아니라면,,

                                                                // formattedDate보다 작은 놈과 큰 놈 사이 index에 집어넣기
                                                                // formattedDate를 삽입할 위치 찾기
                                                                int insertIndex = 0;

                                                                // 리스트를 순회하며 위치 탐색
                                                                for (int i = 0; i < classDates.size(); i++) {
                                                                    String currentDate = classDates.get(i);
                                                                    if (!currentDate.equals("") && currentDate.compareTo(formattedDate) > 0) { // formattedDate보다 큰 값 발견
                                                                        insertIndex = i;
                                                                        break;
                                                                    }
                                                                }

                                                                // formattedDate를 적절한 위치에 삽입
                                                                classDates.add(insertIndex, formattedDate);
                                                                classDates.remove(resetCount);

                                                                // 그에 따른 수업의 start, end 시간도 초기화 -> 나중에 사용자한테 입력 받으라고 해야함.
                                                                Map<String, String> tempTimeMap = new HashMap<>();
                                                                tempTimeMap.put("start", "");
                                                                tempTimeMap.put("end", "");
                                                                classStartEndTimes.add(insertIndex, tempTimeMap);
                                                                classStartEndTimes.remove(resetCount);

                                                                docRef.update("classDates", classDates)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            Toast.makeText(getContext(), koreanDay + " 수업이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Toast.makeText(getContext(), "수업 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        });

                                                                docRef.update("classStartEndTimes", classStartEndTimes)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            Toast.makeText(getContext(), "시간 정보가 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Toast.makeText(getContext(), "시간 초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        });
                                                            }
                                                        }
                                                    }
                                                }
                                            });

                                        })
                                        .setNegativeButton("취소", null)
                                        .create();

                                addClassDialog.show();
                            })
                            .setNegativeButton("삭제", (dialog, which) -> {
                                AlertDialog deleteClassDialog = new AlertDialog.Builder(requireContext())
                                        .setMessage(year + "년 " + koreanDay + " 수업을 삭제하시겠습니까?")
                                        .setPositiveButton("확인", (deleteDialog, deleteWhich) -> {
                                            docRef.get().addOnSuccessListener(documentSnapshot -> {
                                                if (documentSnapshot.exists()) {
                                                    List<String> classDates = (List<String>) documentSnapshot.get("classDates");
                                                    List<Map<String, String>> classStartEndTimes = (List<Map<String, String>>) documentSnapshot.get("classStartEndTimes");
                                                    List<Map<String, String>> classBatchRule = (List<Map<String, String>>) documentSnapshot.get("classBatchRule");
                                                    // 수업 날짜의 개수가 0이라면 삭제하면 안된다.
                                                    int count = 0;
                                                    boolean okay = true;
                                                    for (String dates : classDates) {
                                                        if (Objects.equals(dates, "")) {
                                                            count++;
                                                        }
                                                    }
                                                    if (count == resetCount) {
                                                        Toast.makeText(getContext(), "수업이 아무것도 없습니다.\n" +
                                                                "일괄 생성하거나 수업을 추가하세요.", Toast.LENGTH_SHORT).show();
                                                        okay = false;
                                                    }
                                                    // 아무것도 없으면 실행하지 않는다. 하나라도 있으면 실행
                                                    if (okay) {
                                                        if (classDates != null && classDates.contains(formattedDate)) {
                                                            int removeIndex = classDates.indexOf(formattedDate);
                                                            // 일괄 생성 규칙이 없다면 ""으로 초기화 해주고, 규칙이 있다면 규칙에 맞게 수업을 추가한다.
                                                            // => resetCount에 맞도록 수업 날짜의 개수가 유지되어야 함.
                                                            // 규칙이 있다면 끝에 놈에 대해서 그 다음 수업 날짜를 계산해야함. 그러니 무턱대고 지우면 안되고,
                                                            // 계산하고, 삭제해야하는 놈을 삭제한 후에, resetCount-1에 다음 수업 날짜를 집어 넣는다.
                                                            if (classBatchRule != null) {

                                                                // 2. resetCount-1한 값의 날짜 lastDay 를 가져온다.
                                                                String lastDay = classDates.get(resetCount - 1);
                                                                Log.d("JH", "lastDay : " + lastDay);

                                                                // 3. lastDay에 1씩 증가하면서 classBatchRule 규칙에 있는 요일이 나오길 기대한다.
                                                                Calendar calendar = Calendar.getInstance();
                                                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                                                                try {
                                                                    // lastDay를 기준으로 설정
                                                                    calendar.setTime(dateFormat.parse(lastDay));
                                                                    Map<String, String> timeMap = new HashMap<>();
                                                                    boolean choose = false;

                                                                    while (true) {
                                                                        calendar.add(Calendar.DAY_OF_MONTH, 1); // lastDay 기준으로 날짜 증가
                                                                        String nextDay = dateFormat.format(calendar.getTime());
                                                                        String dayOfWeek = new SimpleDateFormat("E", Locale.getDefault()).format(calendar.getTime());

                                                                        // 규칙에 따라 요일 확인
                                                                        for (Map<String, String> rule : classBatchRule) {
                                                                            if (rule.get("day").equals(dayOfWeek)) { // 규칙과 일치하는 요일 찾기

                                                                                Log.d("JH", "다음 수업 날짜: " + nextDay);
                                                                                timeMap = new HashMap<>();
                                                                                // 규칙에 저장된 시간 가져오기
                                                                                timeMap.put("start", rule.get("start"));
                                                                                timeMap.put("end", rule.get("end"));
                                                                                choose = true;
                                                                                break;
                                                                            }
                                                                        }
                                                                        if (choose) {
                                                                            // 다음 수업 날짜를 resetCount - 1 위치에 설정
                                                                            classDates.remove(removeIndex);
                                                                            classDates.add(resetCount - 1, nextDay);
                                                                            classStartEndTimes.remove(removeIndex);
                                                                            classStartEndTimes.add(resetCount - 1, timeMap); // 배열 개수 맞춰주기
                                                                            break;
                                                                        }
                                                                    }
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }
                                                            } else {
                                                                // 규칙이 없으면 그냥 그놈 삭제하고 뒤에 "" 추가
                                                                classDates.remove(removeIndex);
                                                                classDates.add(resetCount - 1, "");

                                                                classStartEndTimes.remove(removeIndex);
                                                                Map<String, String> tempTimeMap = new HashMap<>();
                                                                tempTimeMap.put("start", "");
                                                                tempTimeMap.put("end", "");
                                                                classStartEndTimes.add(resetCount - 1, tempTimeMap); // 배열 개수 맞춰주기
                                                            }

                                                            docRef.update("classDates", classDates)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Toast.makeText(getContext(), koreanDay + " 수업이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                                                        // CalendarFragment 새로고침
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "수업 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    });

                                                            docRef.update("classStartEndTimes", classStartEndTimes)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Toast.makeText(getContext(), "시간 정보가 초기화되었습니다.", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "시간 초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    });
                                                        } else {
                                                            Toast.makeText(getContext(), koreanDay + "에 수업이 없습니다.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }

                                                }
                                            });
                                        })
                                        .setNegativeButton("취소", null)
                                        .create();

                                deleteClassDialog.show();
                            })
                            .setPositiveButton("취소", null)
                            .create();

                    thisDayCheckDialog.show();
                } else {
                    //Toast.makeText(getContext(), "Single-click detected on " + date, Toast.LENGTH_SHORT).show();
                    // 단일 클릭 동작 추가
                }
                lastClickTime = currentTime; // 클릭 시간 업데이트
            }
        });
    }

    public static class WantColorDecorator implements DayViewDecorator {
        private final Drawable highlightDrawable;
        private final HashSet<CalendarDay> dates;

        public WantColorDecorator(List<CalendarDay> dates, int color) {
            this.highlightDrawable = new ColorDrawable(color); // 원하는 색깔 넣기
            // 0x804169E1
            // 0xFFFFFFFF
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setBackgroundDrawable(highlightDrawable);
        }
    }
}
