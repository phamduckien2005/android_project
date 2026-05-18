package com.example.sqlite;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MessageBoxActivity extends AppCompatActivity {

    private RecyclerView rvMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_box);

        Toolbar toolbar = findViewById(R.id.toolbar_message_box);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rv_messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<Message> messageList = dbHelper.getAllMessages();

        MessageAdapter adapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(adapter);
    }

    public static class Message {
        String title, content, time;
        public Message(String title, String content, String time) {
            this.title = title; this.content = content; this.time = time;
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private final List<Message> list;
        public MessageAdapter(List<Message> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Message m = list.get(position);
            holder.tvTitle.setText(m.title);
            holder.tvContent.setText(m.content);
            holder.tvTime.setText(m.time);
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvTime;
            public ViewHolder(View v) { super(v);
                tvTitle = v.findViewById(R.id.tv_message_title);
                tvContent = v.findViewById(R.id.tv_message_content);
                tvTime = v.findViewById(R.id.tv_message_time);
            }
        }
    }
}