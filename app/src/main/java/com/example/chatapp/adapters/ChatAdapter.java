package com.example.chatapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.chatapp.activities.MapDirectionActivity;
import com.example.chatapp.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatapp.databinding.ItemContainerSentMessageBinding;
import com.example.chatapp.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private  Bitmap receiverProfileImage;
    private final android.content.Context context;



    private final String senderId;

    // user has 2 state is receive and send
    private static final int VIEW_TYPE_SENT = 1;  // when user sends message to the other one
    private static final int VIEW_TYPE_RECEIVED = 2; // when user receives message from the other one



    public void setReceiverProfileImage(Bitmap bitmap){
        receiverProfileImage=bitmap;
    }

    public ChatAdapter(Context context, List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.context = context;
        this.senderId = senderId;

    }




    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            // create a holder for adapter to binding
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceiverMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
            if(chatMessages.get(position).isMap){
                ((SentMessageViewHolder) holder).binding.imageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(context,MapDirectionActivity.class);
                        intent.putExtra("senderId",chatMessages.get(holder.getAdapterPosition()).senderId);
                        intent.putExtra("receiverId",chatMessages.get(holder.getAdapterPosition()).receiverId);
                        intent.putExtra("senderLatitude",chatMessages.get(holder.getAdapterPosition()).lat);
                        intent.putExtra("senderLongitude",chatMessages.get(holder.getAdapterPosition()).lng);
                        context.startActivity(intent);
                        Log.d("senderIddddddddd......",chatMessages.get(holder.getAdapterPosition()).senderId);
                        Log.d("receiverIdddddd......",chatMessages.get(holder.getAdapterPosition()).receiverId);
                        Log.d("senderLattide......",chatMessages.get(holder.getAdapterPosition()).lat);
                        Log.d("senderLongitude......",chatMessages.get(holder.getAdapterPosition()).lng);
                    }
                });
            }

        } else {
            ((ReceiverMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
            if(chatMessages.get(position).isMap){
                ((ReceiverMessageViewHolder) holder).binding.imageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(context,MapDirectionActivity.class);
                        intent.putExtra("senderId",chatMessages.get(holder.getAdapterPosition()).senderId);
                        intent.putExtra("receiverId",chatMessages.get(holder.getAdapterPosition()).receiverId);
                        intent.putExtra("senderLatitude",chatMessages.get(holder.getAdapterPosition()).lat);
                        intent.putExtra("senderLongitude",chatMessages.get(holder.getAdapterPosition()).lng);
                        context.startActivity(intent);
                        Log.d("senderIddddddddd......",chatMessages.get(holder.getAdapterPosition()).senderId);
                        Log.d("receiverIdddddd......",chatMessages.get(holder.getAdapterPosition()).receiverId);
                        Log.d("senderLattide......",chatMessages.get(holder.getAdapterPosition()).lat);
                        Log.d("SenderLongitude......",chatMessages.get(holder.getAdapterPosition()).lng);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {  // check position belong to sender or receiver
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

     static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSentMessageBinding binding; // generated from XML

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {


            if(!chatMessage.image.equals("empty Image")){
                binding.imageMessage.setImageBitmap(getMessageImage(chatMessage.image));
                binding.imageMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setVisibility(View.GONE);
              //  binding.textDateTime.setText(chatMessage.dateTime);

            }else{
                binding.textMessage.setText(chatMessage.message);
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.imageMessage.setVisibility(View.GONE);
            }

        }
    }


    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        ReceiverMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {

            if(!chatMessage.image.equals("empty Image")){
                binding.imageMessage.setImageBitmap(getMessageImage(chatMessage.image));
                binding.imageMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setVisibility(View.GONE);
                //binding.textDateTime.setText(chatMessage.dateTime);
            }else{
                binding.textMessage.setText(chatMessage.message);
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.imageMessage.setVisibility(View.GONE);
            }


            // because receiver has avatar while chatting so we must set avatar
            if(receiverProfileImage!=null){
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }

        }
    }

    private static Bitmap getMessageImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
