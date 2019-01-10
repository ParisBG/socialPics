package com.garrettgray.android.ggphoto;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String LAST_RESULT_ID = "lastResultId";
    private static final String PREF_IS_ALARM_ON = "isAlarmOn";

    static String getStoredQuery(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY,null);
    }
    static void setStoredQuery(Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SEARCH_QUERY,query).apply();
    }

    static String getLastResultId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(LAST_RESULT_ID,null);
    }
    static void setLastResultId(Context context, String lastResultId){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(LAST_RESULT_ID,lastResultId).apply();
    }

    public static boolean isAlarmOn(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_IS_ALARM_ON,false);
    }

    public static void setAlarmOn(Context context,boolean isOn){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_IS_ALARM_ON,isOn).apply();
    }


}
