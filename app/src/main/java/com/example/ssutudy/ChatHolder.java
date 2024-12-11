package com.example.ssutudy;

import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ssutudy.databinding.RowChatBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatHolder extends RecyclerView.ViewHolder {

    private final RowChatBinding binding;

    public ChatHolder(@NonNull RowChatBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(@NonNull Chat chat, @NonNull String currentUserId) {
        binding.nameTextView.setText(chat.getName());
        binding.messageTextView.setText(chat.getMessage());
        if (chat.getTimestamp() != null) {
            String formattedTimestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(chat.getTimestamp().toDate());
            binding.timestampTextView.setText(formattedTimestamp);
        } else {
            binding.timestampTextView.setText(""); // Show nothing if timestamp is null
        }

        if (chat.getUid().equals(currentUserId)) {
            // 본인이 보낸 메시지
            binding.chatLayout.setGravity(Gravity.END);
            binding.messageTextView.setBackgroundResource(R.color.royal_blue);
        } else {
            // 상대방이 보낸 메시지
            binding.chatLayout.setGravity(Gravity.START);
            binding.messageTextView.setBackgroundResource(R.color.light_purple);
        }
    }
}
