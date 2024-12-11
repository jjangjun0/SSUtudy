package com.example.ssutudy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ssutudy.databinding.ChatMemberListBinding;
import com.example.ssutudy.databinding.CsListBinding;
import com.example.ssutudy.databinding.FragmentChattingBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChattingFragment extends Fragment {

    private static class ChatItem {
        private String userId;
        private String userRole;

        ChatItem(String userId, String userRole) {
            this.userId = userId;
            this.userRole = userRole;
        }
    }

    private FragmentChattingBinding binding;

    static SharedPreferences sharedPref;

    private List<ChatItem> chatList;
    private ChattingFragment.ChatAdapter chatAdapter;

    private FirebaseFirestore db;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // ViewBinding으로 레이아웃 연결
        binding = FragmentChattingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        sharedPref = requireActivity().getSharedPreferences("HomeActivity", Context.MODE_PRIVATE); // userEmail 가져오기

        // 어댑터 연결
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        binding.chatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatList.setAdapter(chatAdapter);

        // Firestore에서 데이터 가져오기
        fetchClasses();

    }

    private void fetchClasses() {
        String currentUserEmail = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
        String currentRole = sharedPref.getString("userRole", "noRole"); // default : "noRole"


        if (currentRole.equals("teacher")) {
            // 기존 코드 (선생님인 경우)
            db.collection("accounts")
                    .whereEqualTo("teacherId", currentUserEmail)
                    .whereIn("role", Arrays.asList("parent", "student")) // "parent" 또는 "student"
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        chatList.clear();
                        for (DocumentSnapshot document : querySnapshot) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                String emailId = (String) data.get("emailId");
                                String role = (String) data.get("role");
                                if (emailId != null && role != null) {
                                    ChatItem item = new ChatItem(emailId, role);
                                    chatList.add(item);
                                }
                            }
                        }
                        chatList.sort((item1, item2) -> item1.userId.compareTo(item2.userId));
                        Collections.reverse(chatList);
                        chatAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("JH", "Failed to fetch chatting", e);
                        Toast.makeText(requireContext(), "채팅 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    });
        } else if (currentRole.equals("student") || currentRole.equals("parent")) {
            Log.d("JH", currentRole + " 입니다."); // 생성된 채팅방만 들어갈 수 있다.

            // currentUserEmail이 member2와 일치하는 문서를 가져오기
            db.collection("chats")
                    .whereEqualTo("member2", currentUserEmail)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        // chatList 초기화
                        chatList.clear();

                        // member2가 currentUserEmail과 일치하는 문서 처리
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            addChatItemFromDocument(document);
                        }

                        // chatList를 정렬 (userId 기준 오름차순)
                        chatList.sort(Comparator.comparing(item -> item.userId));

                        // chatList를 역순으로 변경
                        Collections.reverse(chatList);

                        // 어댑터에게 데이터가 변경되었음을 알림
                        chatAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "member2와 일치하는 문서를 가져오는 데 실패했습니다.", e);
                        Toast.makeText(requireContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 아무 동작 안함
            Log.d("JH", "ChattingFragment 아무것도 안함");
        }

    }
    // Firestore 문서에서 ChatItem을 생성해 chatList에 추가하는 헬퍼 함수
    private void addChatItemFromDocument(DocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        if (data != null) {
            String member1 = (String) data.get("member1"); // teacher ID
            String role = "teacher"; // 항상 teacher이므로 고정
            if (member1 != null) {
                ChatItem item = new ChatItem(member1, role);
                chatList.add(item);
            }
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatItem> chatList;

        ChatAdapter(List<ChatItem> chatList) {
            this.chatList = chatList;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ChatMemberListBinding binding = ChatMemberListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ChatViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatItem chatItem = chatList.get(position);
            holder.bind(chatItem);
        }

        @Override
        public int getItemCount() {
            return chatList.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            private final ChatMemberListBinding binding;

            ChatViewHolder(ChatMemberListBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(ChatItem item) {
                // 채팅 인원 -> 일단 한 명이라고 가정.
                String chattingRoomName = item.userId;
                switch (item.userRole) {
                    case "teacher":
                        // userRole이 "teacher"인 경우 실행할 코드
                        Log.d("RoleSwitch", "Teacher role selected");
                        chattingRoomName += " (선생님)";
                        break;

                    case "student":
                        // userRole이 "student"인 경우 실행할 코드
                        Log.d("RoleSwitch", "Student role selected");
                        chattingRoomName += " (학생)";
                        break;

                    case "parent":
                        // userRole이 "parent"인 경우 실행할 코드
                        Log.d("RoleSwitch", "Parent role selected");
                        chattingRoomName += " (학부모)";
                        break;

                    default:
                        // 위의 경우에 해당하지 않는 경우 실행할 코드
                        Log.d("RoleSwitch", "Unknown role");
                        chattingRoomName = " (???) ";
                        break;
                }
                binding.ChattingMember.setText(chattingRoomName);
                // 별 클릭
                binding.chatChooseOne.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 채팅방으로 입장
                        Intent intent = new Intent(getActivity(), ChatRoomActivity.class);

                        intent.putExtra("with", item.userId); // 일단 1:1 채팅을 구현함.
                        // 새로운 인스턴스를 생성하지 않고, 기존의 getActivity 를 스택에서 가져온다.
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                });

                String currentUserEmail = sharedPref.getString("userEmail", "noEmail"); // default: "noEmail"
                String currentRole = sharedPref.getString("userRole", "noRole"); // default : "noRole"
                // 선생님만 채팅방을 지울 수 있음.
                if (currentRole.equals("teacher")) {
                    binding.chatDelete.setOnClickListener(v -> {
                        AlertDialog deleteCheckDialog = new AlertDialog.Builder(requireContext())
                                .setMessage("이 채팅방을 삭제하시겠습니까?")
                                .setPositiveButton("확인", (dialog, which) -> {
                                    // Firestore에서 member1 = currentUserEmail, member2 = item.userId 조건에 맞는 문서 검색
                                    db.collection("chats")
                                            .whereEqualTo("member1", currentUserEmail)
                                            .whereEqualTo("member2", item.userId)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                if (querySnapshot.isEmpty()) {
                                                    Toast.makeText(requireContext(), "해당 채팅방을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                // 첫 번째 문서를 가져옴 (chats 컬렉션 내 해당 채팅방 문서)
                                                DocumentSnapshot chatDocument = querySnapshot.getDocuments().get(0);
                                                String chatRoomId = chatDocument.getId(); // 문서 ID

                                                // Firestore의 messages 하위 컬렉션 삭제
                                                WriteBatch batch = db.batch();

                                                db.collection("chats").document(chatRoomId).collection("messages")
                                                        .get()
                                                        .addOnSuccessListener(messagesSnapshot -> {
                                                            for (DocumentSnapshot messageDocument : messagesSnapshot.getDocuments()) {
                                                                batch.delete(messageDocument.getReference()); // 각 메시지 삭제
                                                            }

                                                            // batch 실행
                                                            batch.commit()
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        chatList.remove(getAdapterPosition()); // RecyclerView에서 채팅방 제거
                                                                        chatAdapter.notifyDataSetChanged(); // RecyclerView 업데이트
                                                                        Toast.makeText(requireContext(), "채팅 내용을 삭제했습니다.", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e("Firestore", "Failed to delete messages in chat room", e);
                                                                        Toast.makeText(requireContext(), "채팅 내용 삭제를 실패했습니다.", Toast.LENGTH_SHORT).show();
                                                                    });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("Firestore", "Failed to fetch messages in chat room", e);
                                                            Toast.makeText(requireContext(), "채팅 내용을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("Firestore", "Failed to fetch chat room", e);
                                                Toast.makeText(requireContext(), "채팅방을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .setNegativeButton("취소", null) // 취소 버튼
                                .create();
                        deleteCheckDialog.show();
                    });
                } else {
                    binding.chatDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(requireContext(), "선생님만 채팅방을 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Binding 객체 해제
        binding = null;
    }
}
