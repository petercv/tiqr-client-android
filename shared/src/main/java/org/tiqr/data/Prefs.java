package org.tiqr.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs 
{
    public static SharedPreferences get(Context context) 
    {
        return context.getSharedPreferences("SA_PREFS", 0);
    }
}