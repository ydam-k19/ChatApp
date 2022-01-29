package com.example.chatapp.activities;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.chatapp.R;
import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.network.ApiClient;
import com.example.chatapp.network.ApiService;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;      // generated from XML file
    private User receiverUser;                // user receiver message
    private List<ChatMessage> chatMessages;   //list messages have been sent to each other
    private ChatAdapter chatAdapter;          // Adapter pattern to bind with recyclerView
    private PreferenceManager preferenceManager; // hold the state
    private FirebaseFirestore database;         //connection to firebase database
    private String conversionId = null;         // last message id
    private Boolean isReceiverAvailable = false;  // online or offline status

    private String encodedImage = "";
    private String lat = "";
    private String lng = "";


    FloatingActionButton fab_add, fab_img, fab_location, fab_filter;
    Animation rotateOpen, rotateClose, fromBottom, toBottom;
    boolean isOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();         // listen to event changed in the screen application
        loadReceiverDetails();  //load conservations before
        init();                 // message conversation not loaded
        listenMessages();       // get messages from database


        //animations
        fab_add = binding.fabAdding;
        fab_img = binding.fabImg;
        fab_location = binding.fabLocation;
        fab_filter = binding.fabFilter;

        rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open);
        rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close);

        fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom);
        toBottom = AnimationUtils.loadAnimation(this, R.anim.to_bottom);

        // set click listener
        fab_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
            }
        });

        fab_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);

                animateFab();

            }
        });

        fab_location.setOnClickListener(view -> {

            Intent intent = new Intent(ChatActivity.this, MapsActivity.class);
            pickLocation.launch(intent);

            animateFab();
        });


        fab_filter.setOnClickListener(view -> {

            Intent intent = new Intent(ChatActivity.this, FilterActivity.class);
            pickImageFilter.launch(intent);

            animateFab();
        });


    }


    private void animateFab() {
        if (!isOpen) {
            fab_add.startAnimation(rotateOpen);
            fab_img.startAnimation(fromBottom);
            fab_location.startAnimation(fromBottom);
            fab_filter.startAnimation(fromBottom);
            fab_img.setClickable(true);
            fab_location.setClickable(true);
            fab_filter.setClickable(true);
            fab_img.setVisibility(View.VISIBLE);
            fab_location.setVisibility(View.VISIBLE);
            fab_filter.setVisibility(View.VISIBLE);
            isOpen = true;
        } else {
            fab_add.startAnimation(rotateClose);
            fab_img.startAnimation(toBottom);
            fab_location.startAnimation(toBottom);
            fab_filter.startAnimation(toBottom);

            fab_img.setClickable(false);
            fab_location.setClickable(false);
            fab_filter.setClickable(false);
            fab_img.setVisibility(View.INVISIBLE);
            fab_location.setVisibility(View.INVISIBLE);
            fab_filter.setVisibility(View.INVISIBLE);
            isOpen = false;
        }
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();       // init List chatMessage
        chatAdapter = new ChatAdapter(
                getApplicationContext(),
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();  //open connection , ready for transactions or retrieve data
    }


    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 500;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            encodedImage = encodeImage(bitmap); // !null


                            binding.imagePreview.setImageBitmap(bitmap);
                            binding.imagePreviewLayout.setVisibility(View.VISIBLE);

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickImageFilter = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {

                        try {
                            encodedImage = result.getData().getStringExtra("img_filter");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickLocation = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        encodedImage = result.getData().getStringExtra("IMAGE_LOCATION");
                        lat = result.getData().getStringExtra("SENDER_LATITUDE");
                        lng = result.getData().getStringExtra("SENDER_LONGITUDE");

                        sendMessage();

                    }
                }
            }
    );


    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID)); // KEY_USER_ID = user that logging in system =user sender
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);     // id of user received message
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString()); // get data from message input and convert to String
        message.put(Constants.KEY_TIMESTAMP, new Date()); // set time send message
        if (encodedImage.isEmpty())
            message.put(Constants.KEY_MESSAGE_IMAGE, "");
        else
            message.put(Constants.KEY_MESSAGE_IMAGE, encodedImage);
        //set latitude
        if (lat.isEmpty() && lng.isEmpty()) {
            message.put(Constants.KEY_LONGITUDE, "");
            message.put(Constants.KEY_LATITUDE, "");
            message.put(Constants.KEY_IS_MAP, false);
            preferenceManager.putBoolean(Constants.KEY_IS_MAP, false);
        } else {
            message.put(Constants.KEY_LATITUDE, lat);
            message.put(Constants.KEY_LONGITUDE, lng);
            message.put(Constants.KEY_IS_MAP, true);
            preferenceManager.putBoolean(Constants.KEY_IS_MAP, true);
        }
        if (message.get(Constants.KEY_MESSAGE).toString().isEmpty() && message.get(Constants.KEY_MESSAGE_IMAGE).toString().isEmpty())
            Toast.makeText(getApplicationContext(), "No input message", Toast.LENGTH_SHORT).show();
        else
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message); // add to collections
        encodedImage = "";
        if (conversionId != null) {
            updateConversion(binding.inputMessage.getText().toString(), preferenceManager.getString(Constants.KEY_USER_ID));
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());

            conversion.put(Constants.KEY_LAST_IS_MAP, message.get(Constants.KEY_IS_MAP));
            conversion.put(Constants.KEY_LAST_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);

        }
        if (!isReceiverAvailable) { // if the receiver is offline-> send notifications
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());

            } catch (Exception exception) {
                Log.d("exception TAG", exception.toString());
            }
        }
        binding.inputMessage.setText(null);
        binding.imagePreviewLayout.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);

                                Log.d("error TAG", error.getString("error"));
                                return;
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d("error TAG", "" + response.code());

                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.d("exception TAG", t.getMessage());

            }
        });
    }

    private void listenAbilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;  // check conditions
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if (receiverUser.image == null) {
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                    chatAdapter.notifyItemRangeChanged(0, chatMessages.size());

                }
            }
            if (isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.GONE);
            }

        });
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }


    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) { // if system get error-> break
            return;
        }
        if (value != null) {
            int count = chatMessages.size();

            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    if (!documentChange.getDocument().getString(Constants.KEY_MESSAGE_IMAGE).isEmpty())
                        chatMessage.image = documentChange.getDocument().getString(Constants.KEY_MESSAGE_IMAGE);
                    else
                        chatMessage.image = "";

                    if (!documentChange.getDocument().getString(Constants.KEY_LATITUDE).isEmpty())
                        chatMessage.lat = documentChange.getDocument().getString(Constants.KEY_LATITUDE);
                    else
                        chatMessage.lat = "";

                    if (!documentChange.getDocument().getString(Constants.KEY_LONGITUDE).isEmpty())
                        chatMessage.lng = documentChange.getDocument().getString(Constants.KEY_LONGITUDE);
                    else
                        chatMessage.lng = "";

                    chatMessage.isMap = documentChange.getDocument().getBoolean(Constants.KEY_IS_MAP);


                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }

            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));

            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);

        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (!encodedImage.isEmpty()) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;

    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        byte[] bytes = Base64.decode(receiverUser.image, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.send.setOnClickListener(v -> {
//            if(!binding.inputMessage.getText().toString().isEmpty())
            sendMessage();
        });

        binding.removeImagePreview.setOnClickListener(v -> {
            encodedImage = "";
            binding.imagePreview.setImageBitmap(null);
            binding.imagePreviewLayout.setVisibility(View.GONE);
        });

        binding.inputMessage.setOnFocusChangeListener((v, hasFocus) -> {
            binding.layoutSend.setBackgroundResource(R.drawable.background_chat_input_on_focus);
            binding.send.setColorFilter(ContextCompat.getColor(ChatActivity.this, R.color.primary), android.graphics.PorterDuff.Mode.MULTIPLY);
            View view = this.getCurrentFocus();
            if (view != binding.inputMessage) {
                binding.layoutSend.setBackgroundResource(R.drawable.background_chat_input);
                binding.send.setColorFilter(ContextCompat.getColor(ChatActivity.this, R.color.secondary_text), android.graphics.PorterDuff.Mode.MULTIPLY);
            }
        });

        binding.inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //            if(!binding.inputMessage.getText().toString().isEmpty())

                sendMessage();
                return true;
            }
            return false;
        });
    }

    //    clear focus on touch outside editText
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);

                View icon = binding.send;
                Rect iconRect = new Rect();
                icon.getGlobalVisibleRect(iconRect);

                outRect.right += iconRect.right - outRect.left;
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd,yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message, String lastSenderId) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId); // return document


        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date(),
                Constants.KEY_LAST_IS_MAP, preferenceManager.getBoolean(Constants.KEY_IS_MAP),
                Constants.KEY_LAST_SENDER_ID, lastSenderId

        );
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receivedId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receivedId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAbilityOfReceiver();
    }


}