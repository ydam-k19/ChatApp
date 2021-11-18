package com.example.chatapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {  // preferenceManger used to help create Preference hierarchies from activities or XML.
    private final SharedPreferences sharedPreferences;  // storing or retrieve data key/value pairs

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public void putBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply(); // like commit
    }

    public Boolean getBoolean(String key){
        return  sharedPreferences.getBoolean(key,false);
    }

    public void putString(String key, String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public String getString(String key){
        return sharedPreferences.getString(key,null);
    }

    public void clear(){  // clear after using SharedPreference
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
