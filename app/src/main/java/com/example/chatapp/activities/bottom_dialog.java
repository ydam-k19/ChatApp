package com.example.chatapp.activities;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chatapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class bottom_dialog extends BottomSheetDialogFragment {
    private TextView title;
    private Button btn_visit;
    private String fetchurl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_dialog,container,false);

        title = view.findViewById(R.id.txt_title);
        btn_visit = view.findViewById(R.id.visit);

        title.setText(fetchurl);

        btn_visit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse(fetchurl));
                startActivity(intent);
            }
        });

//        close.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                dismiss();
//            }
//        });

        return view;

    }

    public void fetchurl(String url){
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executorService.execute(new Runnable(){
            @Override
            public void run() {
                fetchurl = url;
            }
        });
    }
}

