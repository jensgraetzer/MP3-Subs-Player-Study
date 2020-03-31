package com.example.a068mp3playerst;

// Jens Grätzer, 2020-03-31
// Design Study "MP3 Subs Player"

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "xxxMainActivity";
    final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 747;
    Context cntxt;
    //ArrayList<Album> albumList;
    ArrayList<String> albumList;
    ArrayAdapter albumArrayAdapter;
    ListView lvAlbum;
    Button btnInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "Running onCreate()");

        //cntxt = this;                   // Diese Angabe des Context funktioniert.
        cntxt = getApplicationContext();  // Diese andere Angabe des Context funktioniert auch.
        lvAlbum = findViewById(R.id.lvAlbum);
        btnInfo = findViewById(R.id.btnInfo);

        // OnClick-Listener für den Button "Info"
        btnInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //show Info-Dialog
                // See: https://stackoverflow.com/questions/2115758/how-do-i-display-an-alert-dialog-on-android
                openInfoDialog();
            }
        });

        // OnItemClick-Listener für die Listen-Einträge zufügen
        lvAlbum.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long arg3) {
                String itemTextValue = (String) parent.getItemAtPosition(position);
                Log.i(TAG, "Item Position = " + position + " ... " + itemTextValue);
                // Speichere den String des gewählten Listenelments, d.h. das gewählte Album
                Data.storeAlbum = itemTextValue;
                // Starte hier die nächste Activity. Dort gibt es den ListView für die Titel-Auswahl.
                startTitleActivity();
            }
        });

        // Self-Permission, see:
        // https://developer.android.com/training/permissions/requesting
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            getAlbumsFromContentProvider(cntxt);
        }
    }

    // gehört auch zur Self-Permission
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the task you need to do.
                    getAlbumsFromContentProvider(cntxt);

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void getAlbumsFromContentProvider(final Context context) {

        Log.i(TAG, "Running getAlbumsFromContentProvider()");

        // MediaStore - See:
        // https://stackoverflow.com/questions/47258073/how-to-get-list-of-all-music-files-from-android-phone?rq=1

        // ===> WOW: Get URI or all files instead only of Images/Audio/Video - See:
        // https://stackoverflow.com/questions/10384080/mediastore-uri-to-query-all-types-of-files-media-and-non-media
        //Uri uri = MediaStore.Files.getContentUri("external");
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("txt");  // WOOWW :))
        Log.i(TAG, "  mimeType=" + mimeType);             // ... mimeType von "srt" ist unbekannt :( .... passt "text/srt"?!!
        // ... lies diese AKTUELLE Dokumentation darüber, wie man URI verwndet:
        // https://developer.android.com/training/data-storage/shared/media
        // ... siehe danach vvlt.,, wie man URI und ID verbindet (uri = ContentUris.withAppendedId(contentUri, id);) in:
        // https://www.codota.com/code/java/classes/android.provider.MediaStore$Files

        // Managing Files im Emulator und auf angeschlossenen Geräten:
        // Nimm im Android Studio den Device File Explorer: View > Tool Windows > Device File Explorer or click the Device File Explorer button
        // https://developer.android.com/studio/debug/device-file-explorer

        // Die Bestandteile der Abfrage zusammenbauen:
        // URI  der abzufragenden Tabelle
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //Nenne die Felder für das Abfrage-Ergebnis
        String[] projection = {
                //"DISTINCT " + MediaStore.Audio.Albums.ALBUM    // Mit zusätzl. "DISTINCT " Duplikate verhindern <- OK in Android 8, crasht in Android 10
                MediaStore.Audio.Albums.ALBUM    // Ohne "DISTINCT " kein Crash in Android 10, aber Doppelgänger <- Verhindere Duplukate beim Listeneintrag!
                //MediaStore.Audio.Albums.ALBUM
                //MediaStore.Audio.AlbumColumns.ALBUM,          // Komisch, gibt es viele Tabellen mit "ALBUM"? Ist eine davon eindeutig?
                //MediaStore.Audio.AudioColumns.ALBUM,
                //MediaStore.Video.Media.ALBUM,
                //MediaStore.Audio.Albums._ID                   // _ID in der Tabelle ist für Audio-Titel
        };

        // Wie vermeidet man Duplikate im Abfrage-Ergebnis?
        // Siehe: https://stackoverflow.com/questions/22190876/how-to-use-mediastore-query-to-get-artists-without-duplicates

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";  // Filter: Damit sind dann im Ergebnis "Ringtones" etc. entfernt.
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE ASC";    // Es gibt ASC, DESC ... COLLATE NOCASE ASC sortiert case insensitive

        //// To read the files of a specific folder, use this query (write the target folder name in the query)
        //Cursor c = context.getContentResolver().query(
        //        uri,
        //        projection,
        //        MediaStore.Audio.Media.DATA + " like ? ",   // Selection
        //        new String[]{"%yourFolderName%"},           // Selection Args
        //        null);                                      // Sort Order

        // If you want all the files on the device, use this query:
        Cursor c = context.getContentResolver().query(
                uri,
                projection,
                selection,
                null,
                sortOrder);

        // Wir gehen hier einen Umweg: Zuerst stellen wir eine Liste der Alben her.
        // Diese verbinden wir danach dann über einen Adapter mit dem ListView.
        albumList = new ArrayList<String>();
        if (c != null) {
            Log.i(TAG, "c != null");
            Log.i(TAG, "c.getCount()=" + c.getCount());

            while (c.moveToNext()) {
                //Log.i(TAG, "Running c.moveToNext()");

                // Das funktioniert ja normalerweise, denn Android nutzt UTF-8 als Standard ...
                String album = c.getString(0);  // UTF-8 kodierten Inhalt in String lesen

                /* Obige Lösung funktioniert auf dem Emulator "Nox Player" allerdings nicht:
                   Polnische Sonderzeichen erscheinen dann als zwei Unsinns-Zeichen auf dem Bildschirm
                //
                //// ... der "Nox Player" verwendet UTF-8 allerdings offenbar nicht als Standard?
                ////       Mit dieses einfachen Umwandelung wird das Problem nicht gelöst:
                // album = URLDecoder.decode(album, "UTF-8");

                // ... also werden wir zuerst die Bytes lesen und diese in ein UTF-8 String zu wandeln:
                byte[] bytes = c.getBlob(0);
                String album = "";
                try {
                    album = new String(bytes, "UTF-8");
                    album = album.trim();  // Entferne CR und LF
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // ... leider ergibt das auf dem "Nox Player" kein anderes (besseres) Ergebnis :(
                */

                Log.i(TAG, "  Album: " + album);
                if (!albumList.contains(album))  // Album eintragen, aber Duplikate verhindern
                    albumList.add(album);
            }
            Log.i(TAG, "Number of albums=" + albumList.size());

            // Adapter herstellen, der albumList benutzt. Diesen mit dem myListView verbinden.
            albumArrayAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1,
                    albumList);
            //ListView lvAlbum = (ListView)findViewById(R.id.lvAlbum);
            lvAlbum.setAdapter(albumArrayAdapter);

        } else {
            Log.i(TAG, "ERROR c == null");
        }

        c.close();
    }

    public void startTitleActivity() {
        Intent intent = new Intent(this, TitleActivity.class);
        startActivity(intent);
    }


    public void openInfoDialog() {
        // Alert-Dialog, see: https://stackoverflow.com/questions/2115758/how-do-i-display-an-alert-dialog-on-android
        final Dialog dialog = new Dialog(this); // Context, this, etc.
        dialog.setContentView(R.layout.dialog_info);
        dialog.setTitle(R.string.dialog_title);
        dialog.show();
    }
}

