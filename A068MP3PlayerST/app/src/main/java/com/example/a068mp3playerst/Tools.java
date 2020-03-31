package com.example.a068mp3playerst;

import android.util.Log;

public class Tools {

    /**
     * Converts a duration given as string 00:23:45,678 into milliseconds (integer).
     * Returns 0 on data problems.
     * @param timestamp a string like 00:23:45,678
     * @return the duration in milliseconds
     */
    static int convertDurationStringToMs(String timestamp) {
        int t = 0;

        String[] part = timestamp.split(":", 3);

        if(part.length != 3)
            return 0;   // Returns 0, if timestamp has not 3 parts, as 00:02:34,56

        part[2] = part[2].replace(",", ".");
        //Log.i("Tools", "part[2]="  + part[2]);

        try {
            t = ((Integer.parseInt(part[0]) * 60
                    + Integer.parseInt(part[1])) * 60000)
                    + (int) (Double.parseDouble(part[2]) * 1000);

            //Log.i("Tools", "     ms="  + (int) (Double.parseDouble(part[2]) * 1000));
        } catch(Exception e) {}

        return t;
    }
}
