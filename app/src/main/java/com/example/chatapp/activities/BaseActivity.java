package com.example.chatapp.activities;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;  // handle specific document, particularly It is user info (RETURNED DOCUMENT)

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager=new PreferenceManager(getApplicationContext());
        FirebaseFirestore database=FirebaseFirestore.getInstance(); // create connection to firebase
        documentReference=database.collection(Constants.KEY_COLLECTION_USERS)  // retrieve data first we select the collection and then document
                                  .document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    @Override
    protected void onPause() {  // when we not using this app
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABILITY,0);
    }

    @Override
    protected void onResume() { // comeback and use app
        super.onResume();
        documentReference.update(Constants.KEY_AVAILABILITY,1);
    }
}
