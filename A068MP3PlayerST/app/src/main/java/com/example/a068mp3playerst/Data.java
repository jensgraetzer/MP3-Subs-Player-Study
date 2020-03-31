package com.example.a068mp3playerst;

// Daten, die Activity-übergreifend benutzt werden. Dazu sind sie "static".

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;

import java.util.ArrayList;

public class Data {
    static String storeAlbum;   // Im ersten ListView gewähltes Album aus dem MediaStore
    //static String storeTitle;   // Im zweiten ListView gewählter Titel aus dem Album
    //static Long storeId;   // _ID des im MediaStore ausgewählten Titel - daraus kann dessen URI gebildet werden
    //static Uri titleUri;   // URi des im MediaStore ausgewählten Titel
    static int arrayListPosition = 0;  // Pos. Nr. des Titels in der Liste der Titel des aktuellen Albums: Für Abspiel
    static ArrayList<Title> TitleList = null;  // Liste der Titel des aktuell betretenen Albums
    static MediaPlayer mediaPlayer = null;  // "static" verhindert, dass im onCreate() der PlayerActivity nach BS-Rotation mehrere Player-Objekte entstehen
    static ArrayList<SubtLine> SubtList = null;  // Liste der Untertitel des aktuellen Audio-Titels
}

