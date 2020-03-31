package com.example.a068mp3playerst;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TitleActivity extends AppCompatActivity {
    private static final String TAG = "xxxTitleActivity";
    TextView tvAlbum;
    ListView lvTitle;
    Context cntxt;
    //ArrayList<Title> titleList;  // TODO: Speichere titleList statisch in Data, damit die PlayerActivity es findet!
    ArrayAdapter titleArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        Log.i(TAG, "Running onCreate()");

        // Show the actual album name on display
        tvAlbum = findViewById(R.id.tvAlbum);
        tvAlbum.setText(Data.storeAlbum);

        //cntxt = this;                   // Diese Angabe des Context funktioniert.
        cntxt = getApplicationContext();  // Diese andere Angabe des Context funktioniert auch!
        lvTitle = findViewById(R.id.lvTitle);

        // OnItemClick-Listener für die Listen-Einträge zufügen
        lvTitle.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long arg3) {
                Title itemValue = (Title) parent.getItemAtPosition(position);
                Log.i(TAG, "Item Position = " + position + " ... " + itemValue.Title);
                // Speichere die Daten zum gewählten Listenelments, d.h. zum gewählten Titel
                //Data.titleUri = itemValue.TitleUri; // Diesen Wert nicht übergeben, sondern im Player über "Data.arrayListPosition" ermitteln
                //Data.storeTitle = itemValue.Title;  // Diesen Wert nicht übergeben, sondern im Player über "Data.arrayListPosition" ermitteln
                //Data.storeId = itemValue.Id;        // Diesen Wert nicht übergeben, sondern im Player über "Data.arrayListPosition" ermitteln
                Data.arrayListPosition = position;
                // TODO: Im Player die id, Title, etc aus Listenposition "position" ermitteln - dort muss man den Folgetitel spielen können!
                // Start der Activity, die den gewählten Titel abspielt.
                startPlayerActivity();
            }
        });

        // Run the whole action
        getTitlesFromContentProvider(cntxt);  // get all Audiofile Uri in the TitleList
        getMetasFromContentProvider(cntxt);  // get the Metadatafile Uri in the TitleList - for each Audiofile

        // Beendet ein ggf. schon laufendes Spiel und gibt die Ressourcen frei
        if(Data.mediaPlayer != null) {
            Data.mediaPlayer.release();  // Bringt das MediaPlayer-Objekt in den End-State -> Beendet Spiel
            Data.mediaPlayer = null;
        }
    }

    public void getTitlesFromContentProvider(final Context context) {

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
        Uri tableUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        //Nenne die Felder für das Abfrage-Ergebnis
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,         // Kein zusätzl. "DISTINCT ": Duplikate also zulassen
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.COMPOSER
                //MediaStore.Audio.AlbumColumns.ALBUM,          // Komisch, gibt es viele Tabellen mit "ALBUM"?
                //MediaStore.Audio.AudioColumns.ALBUM,
                //MediaStore.Video.Media.ALBUM,
                //MediaStore.Audio.Albums._ID                   // _ID in der Tabelle ist für Audio-Titel
        };

        // Wie vermeidet man Duplikate im Abfrage-Ergebnis?
        // Siehe: https://stackoverflow.com/questions/22190876/how-to-use-mediastore-query-to-get-artists-without-duplicates

        //String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";  // Das brauche ich hier nicht?
        String selection = MediaStore.Audio.Media.ALBUM + " = ? ";  // Titel von diesem Album
        String[] selectionArgs = {Data.storeAlbum};  // This defines a one-element String array for one selection argument.
        String sortOrder = MediaStore.Audio.Media.TRACK + " ASC";    // ASC, DESC, COLLATE NOCASE ASC <-for case insensitive

        //// To read the files of a specific folder, use this query (write the target folder name in the query)
        //Cursor c = context.getContentResolver().query(
        //        uri,
        //        projection,
        //        MediaStore.Audio.Media.DATA + " like ? ",   // Selection
        //        new String[]{"%yourFolderName%"},           // Selection Args
        //        null);                                      // Sort Order

        // If you want all the files on the device, use this query:
        Cursor c = context.getContentResolver().query(
                tableUri,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        // Wir gehen hier einen Umweg: Zuerst stellen wir eine Liste der Alben her.
        // Diese verbinden wir danach dann über einen Adapter mit dem ListView.
        Data.TitleList = new ArrayList<Title>();
        if (c != null) {
            Log.i(TAG, "c != null");
            Log.i(TAG, "c.getCount()=" + c.getCount());

            while (c.moveToNext()) {
                //Log.i(TAG, "Running c.moveToNext()");
                Title titleObject = new Title();
                Long idLong = c.getLong(0);
                titleObject.setId(idLong);

                // Gewinne auch gleich die TitleURI aus Tabellen-URI und ID, mit einer dieser Methoden:
                // 1) Uri tableUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                //    Uri titleUri = Uri.withAppendedPath (tableUri, idString);     // nicht so "modern"
                // 2) Uri titleUri = ContentUris.withAppendedId(tableUri, idLong);  // besser
                // Siehe unbedingt: https://developer.android.com/guide/topics/media/mediaplayer
                Uri titleUri = ContentUris.withAppendedId(tableUri, idLong);   // COOL KNOWHOW.
                //Uri titleUri = Uri.withAppendedPath (tableUri, c.getString(0));  // ... der alte Weg
                titleObject.setTitleUri(titleUri);  // Mit der URI kann der Media Player den Titel spielen.
                // (Frage: kann man mit der URI auch die MP3 Datei öffnen und bytweise lesen - ID3 parsen???)

                titleObject.setTitle(c.getString(1));
                titleObject.setAlbum(c.getString(2));

                String artist = c.getString(3);  // Hier wird offenbar UTF-8 geliefert, aber manche Geräte machen ASCII Latin 1 draus.
                /*
                // Versuch, den String byteweise zu reparieren - funktioniert auf den betroffenen Geräten, zuerstört Funktion auf den anderen Geräten.
                //
                byte[] b = new byte[0];
                //b = artist.getBytes();
                //Log.i(TAG, "zzzz Artist Bytes=" + b[0] + "." + b[1] + "." + b[2] + "." + b[3] + "." + b[4] + ":" + b[5] + "." + b[6] + "." + b[7] + "." + b[8] + "." + b[9] + ":" + b[10] + "." + b[11] + "." + b[13] + "." + b[14] + "." + b[15]);
                try {
                    b = artist.getBytes("ISO-8859-1");

                    Log.i(TAG, "zzz Artist Bytes=" + b[0] + "." + b[1] + "." + b[2] + "." + b[3] + "." + b[4] + ":" + b[5] + "." + b[6] + "." + b[7] + "." + b[8] + "." + b[9] + ":" + b[10] + "." + b[11] + "." + b[13] + "." + b[14] + "." + b[15]);

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String n = "";
                try {
                    newArtist = new String(b, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                 */

                /*
                // Alternative Beschaffung der ID3 Metadaten ... liefert denselben String mit demselben Fehler: auf manchen Geräten als charset 8859-1.
                MediaMetadataRetriever metaDataRetr = new MediaMetadataRetriever();
                metaDataRetr.setDataSource(this, titleUri);
                String artist2 = "";
                try {
                    artist2 = ("WAY2 ") + metaDataRetr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                } catch (Exception e){
                    // Do something if you want
                }
                 */
                titleObject.setArtist(artist);  // Use artist or artist2 ... same result, same bug

                titleObject.setTrack(c.getString(4));
                titleObject.setDisplayName(c.getString(5));
                //titleObject.setComposer(c.getString(6));

                String composer = c.getString(6);
                /*
                // Alternative Beschaffung der ID3 Metadaten ... liefert denselben String mit demselben Fehler: auf manchen Geräten als charset 8859-1.
                MediaMetadataRetriever metaDataRetr = new MediaMetadataRetriever();
                metaDataRetr.setDataSource(this, titleUri);
                String composer2 = "";
                try {
                    composer2 = metaDataRetr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
                } catch (Exception e){
                    // Do something if you want
                }
                */
                titleObject.setComposer(composer);  // Use composer or composer2 ... same result, same bug

                // Schönheitskorrektur: Fehlender Eintrag in der MP3 Datei bewirkt störenden Inhalt "<unknown>"
                if(titleObject.getAlbum().contains("<unknown>"))
                    titleObject.setAlbum("");
                if(titleObject.getArtist().contains("<unknown>"))  // Artist erscheint in 2. Zeile von Title List
                    titleObject.setArtist("");

                // Quick and Dirty (Auch komisch: Es durfte "null" in "Composer" vorkommen):
                // Eine Markierung in String "Artist" eintragen, die angibt, dass es hier Subtitle gibt
                if(titleObject.getComposer() != null) {
                    String composerStr = titleObject.getComposer();
                    Log.i(TAG, String.format("  Composer found: %s", composerStr));
                    // Wenn Zeichen in "Composer" exist. und darin zu Beginn ein ":" vorkommt, sind das vermutl. Subtitle
                    //if(composer.length() > 20 && composer.substring(0,6).contains(":"))
                    if(composerStr.length() > 20 && composerStr.substring(0,8).contains("00:"))
                    // ... dann die Markierung ■ für "Subtitle vorhanden" hinzufügen
                       titleObject.setArtist(String.format("■  %s", titleObject.getArtist()));
                }

                //Den Datensatz in die Subtitel-Liste hinzufügen
                Data.TitleList.add(titleObject);
                Log.i(TAG, String.format("  Title: %s", c.getString(1)));
            }
            Log.i(TAG, "Number of albums=" + Data.TitleList.size());

            // Adapter herstellen, der albumList benutzt. Diesen mit dem myListView verbinden.
            titleArrayAdapter = new TitleAdapter(this,
                    android.R.layout.simple_list_item_2,
                    Data.TitleList);
            //ListView lvAlbum = (ListView)findViewById(R.id.lvAlbum);
            lvTitle.setAdapter(titleArrayAdapter);

        } else {
            Log.i(TAG, "ERROR Audio c == null");
        }

        c.close();
    }

    public void getMetasFromContentProvider(final Context context) {
        Log.i(TAG, "Running getMetasFromContentProvider()");

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
        //Uri filesUri = MediaStore.Files.EXTERNAL_CONTENT_URI; // So geht es nicht? Warum?
        Uri filesUri = MediaStore.Files.getContentUri("external");  // So geht es, liefert aber leeren Cursor

        //Nenne die Felder für das Abfrage-Ergebnis
        String[] projection = {
                MediaStore.Files.FileColumns._ID,    // Damit erhält man die ID - auch von Bildern etc: TODO: Das ist hier unnötig!
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA
        };

        //String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";  // Das brauche ich hier nicht
        String selection = MediaStore.Audio.Media.ALBUM + " = ? ";  // Titel von diesem Album
        String[] selectionArgs = {Data.storeAlbum};  // This defines a one-element String array for one selection argument.
        String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME + " ASC";    // ASC, DESC, COLLATE NOCASE ASC <-for case insensitive


        //// To read the files of a specific folder, use this query (write the target folder name in the query)
        //Cursor c = context.getContentResolver().query(
        //        uri,
        //        projection,
        //        MediaStore.Audio.Media.DATA + " like ? ",   // Selection
        //        new String[]{"%yourFolderName%"},           // Selection Args
        //        null);                                      // Sort Order

        // If you want all the files on the device, use this query:
        //selection = null;                                     // Ohne Filter sind hier auch Bilder etc. im Ergebnis.
        selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";  // Filter: Damit sind Bilder etc. nicht mehr im Ergebnis.
        selectionArgs = null;
        sortOrder = null;
        Cursor c = context.getContentResolver().query(
                filesUri,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        // Gib aus, was gefunden wurde
        if (c != null) {
            Log.i(TAG, "c != null");
            Log.i(TAG, "c.getCount()=" + c.getCount());

            while (c.moveToNext()) {
                //Log.i(TAG, "Running c.moveToNext()");
                Long idLong = c.getLong(0);
                String NameString = c.getString(1);
                String PathString = c.getString(2);
                Log.i(TAG, "  Meta_ID=" + idLong + " Name=" + NameString + " Path=" + PathString);
            }
        } else {
            Log.i(TAG, "ERROR Meta c == null");
        }

        c.close();

    }

    public void startPlayerActivity() {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }
}


