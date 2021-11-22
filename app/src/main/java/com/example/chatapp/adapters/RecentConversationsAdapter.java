package com.example.chatapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ItemContainerRecentConversionBinding;
import com.example.chatapp.listeners.ConversionListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder>{

    private final List<ChatMessage> chatMessages;
    public List<ChatMessage>chatMessagesCopy;
    private final ConversionListener conversionListener;
    private PreferenceManager preferenceManager;
    private Context context;


    public RecentConversationsAdapter(Context context,List<ChatMessage> chatMessages, ConversionListener conversionListener){
        this.context=context;
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
        preferenceManager=new PreferenceManager(context);
        this.chatMessagesCopy=new ArrayList<>();


    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversionBinding binding;
        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding){
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage){
            String receiver_img="you received an image";
            String receiver_location="you received a location";
            String sender_img="you sent an image";
            String sender_location="you sent a location";
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            //if text!=null

            Log.d("textRecent",String.valueOf(chatMessage.isMap));
            binding.textRecentMessage.setText(chatMessage.message);
            if(!chatMessage.message.isEmpty()){

                if(!chatMessage.receiverId.equals(preferenceManager.getString(Constants.KEY_USER_ID)))
                    binding.textRecentMessage.setText(chatMessage.message);
                else
                    binding.textRecentMessage.setText("You: "+chatMessage.message);

            }

            else if(!chatMessage.isMap) {
                if(!chatMessage.receiverId.equals(preferenceManager.getString(Constants.KEY_USER_ID)))
                    binding.textRecentMessage.setText(receiver_img);
                else
                    binding.textRecentMessage.setText(sender_img);
            }
            else
            {
                if(!chatMessage.receiverId.equals(preferenceManager.getString(Constants.KEY_USER_ID)))
                    binding.textRecentMessage.setText(receiver_location);
                else
                    binding.textRecentMessage.setText(sender_location);
            }

            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public void filter(CharSequence charSequence){
        List<ChatMessage>tempArrayList=new ArrayList<>();
        if(!TextUtils.isEmpty(charSequence)){
            for(ChatMessage data:chatMessages){
                if(data.conversionName.toLowerCase().contains(charSequence)){
                    tempArrayList.add(data);
                }
            }
        }
        else{
            tempArrayList.addAll(chatMessagesCopy);
        }

        chatMessages.clear();
        chatMessages.addAll(tempArrayList);
        notifyDataSetChanged();
        tempArrayList.clear();
        Log.d("chat message copy",String.valueOf(chatMessagesCopy.size()));
        Log.d("value search",String.valueOf(chatMessages.size()));
    }
}
