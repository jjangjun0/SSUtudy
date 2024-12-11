package com.example.ssutudy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ssutudy.databinding.ActivityChatRoomBinding;
import com.example.ssutudy.databinding.RowChatBinding;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private ActivityChatRoomBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private FirestoreRecyclerAdapter<Chat, ChatHolder> adapter;
    private String chatRoomId;
    private String currentUserId;
    private String userRole;

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ViewBinding 초기화
        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase 초기화
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getUid();

        // SharedPreferences에서 데이터 가져오기
        sharedPref = getSharedPreferences("HomeActivity", Context.MODE_PRIVATE);
        String user_email = sharedPref.getString("userEmail", "noEmail");
        userRole = sharedPref.getString("userRole", null);

        if (user_email.equals("noEmail") || userRole == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 채팅 상대 설정
        String member1 = user_email;
        String member2 = getIntent().getStringExtra("with");

        // 채팅방 ID 설정 및 메시지 로드
        initializeChatRoom(member1, member2);
        // 메세지 전송 리스너
        binding.sendButton.setOnClickListener(v -> setupListeners());
        // 뒤로가기
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatRoomActivity.this, HomeActivity.class);
                // 새로운 인스턴스를 생성하지 않고, 기존의 HomeActivity 를 스택에서 가져온다.
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initializeChatRoom(String member1, String member2) {
        db.collection("chats")
                .whereIn("member1", Arrays.asList(member1, member2))
                .whereIn("member2", Arrays.asList(member1, member2))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // 기존 채팅방이 있는 경우
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        chatRoomId = document.getId();
                        Log.d("ChatRoomActivity", "Existing chatRoomId: " + chatRoomId);
                        loadMessages();
                    } else {
                        // 기존 채팅방이 없으면 새로운 채팅방 생성
                        createChatRoom(member1, member2);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatRoomActivity", "Error checking chat room", e);
                    Toast.makeText(this, "채팅방 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createChatRoom(String member1, String member2) {
        // 새 채팅방 ID 생성
        String newChatRoomId = db.collection("chats").document().getId();

        // 새 채팅방 문서 참조
        DocumentReference chatRoomRef = db.collection("chats").document(newChatRoomId);

// 첫 번째 필드 추가
        chatRoomRef.set(new HashMap<String, Object>() {{
                    put("member1", member1);
                }})
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatRoomActivity", "member1 추가 성공");

                    // 두 번째 필드 추가
                    chatRoomRef.update("member2", member2)
                            .addOnSuccessListener(aVoid2 -> Log.d("ChatRoomActivity", "member2 추가 성공"))
                            .addOnFailureListener(e -> Log.e("ChatRoomActivity", "member2 추가 실패", e));
                })
                .addOnFailureListener(e -> Log.e("ChatRoomActivity", "member1 추가 실패", e));

}

    private void loadMessages() {
        Log.d("ChatRoomActivity", "Loading messages for chatRoomId: " + chatRoomId);

        Query query = db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp");

        FirestoreRecyclerOptions<Chat> options = new FirestoreRecyclerOptions.Builder<Chat>()
                .setQuery(query, Chat.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Chat, ChatHolder>(options) {
            @NonNull
            @Override
            public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                RowChatBinding rowBinding = RowChatBinding.inflate(getLayoutInflater(), parent, false);
                return new ChatHolder(rowBinding);
            }

            @Override
            protected void onBindViewHolder(@NonNull ChatHolder holder, int position, @NonNull Chat model) {
                holder.bind(model, currentUserId);
            }
        };

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        adapter.startListening();
    }

    private void sendMessage(String content) {
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            Log.e("ChatRoomActivity", "ChatRoomId is null or empty. Cannot send message.");
            Toast.makeText(this, "채팅방 ID가 유효하지 않습니다. 다시 시도하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare chat object with a timestamp
        Chat chat = new Chat(
                setNickname(mAuth.getCurrentUser().getEmail()),
                content,
                currentUserId
        );
        chat.setTimestamp(new com.google.firebase.Timestamp(new java.util.Date())); // Set current timestamp

        // Add the message to the Firestore messages sub-collection
        db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .add(chat)
                .addOnSuccessListener(documentReference -> {
                    Log.d("ChatRoomActivity", "Message sent successfully");
                    Toast.makeText(this, "메시지가 전송되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatRoomActivity", "Failed to send message", e);
                    Toast.makeText(this, "메시지 전송 실패. 다시 시도하세요.", Toast.LENGTH_SHORT).show();
                });
    }


    private void setupListeners() {
        // Set listener for the send button
        binding.sendButton.setOnClickListener(v -> {
            String chatContent = binding.chatContent.getText().toString().trim();
            if (!chatContent.isEmpty()) {
                sendMessage(chatContent);  // Call the sendMessage method to send the chat
                binding.chatContent.setText("");  // Clear the input field after sending
            } else {
                Toast.makeText(this, "메시지를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set listener for the back button
        binding.backBtn.setOnClickListener(v -> onBackPressed());
    }

    private String setNickname(String email) {
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, atIndex) : "Anonymous";
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
