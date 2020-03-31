package com.example.a068mp3playerst;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "xxxPlayerActivity";
    TextView tvAlbum;
    TextView tvTitle;
    TextView tvAlbumTitle;
    TextView tvSubtitle;
    //TextView tvTitlePosition;
    ProgressBar progressBar;
    ImageButton btnNextTitle;
    ImageButton btnPreviousTitle;
    ImageButton btnNextSubt;
    ImageButton btnPreviousSubt;
    static ImageButton btnPlayPause; // "static" deshalb, damit onCreate() nach Rotation keine völlig neue Referenz herstellt
    static ImageButton btnPlaySingle;
    Context cntxt;
    //MediaPlayer mediaPlayer = null;
    static int nextTitle = -1;       // Verschiedene "static" Typen, damit onCreate() bei Rotation keine völlig neuen Variablen bildet
    static int previousTitle = -1;
    static int currentTitle = - 1;
    static int duration100Ms;
    static int timerDevider = 20;
    static int timerDeviderTmp = 0;
    static CountDownTimer tmr;
    static Boolean spIsPlaying = false;
    static String realComposer = "";

    // Handler zur periodischen Untertitel-Aktualisierung realisieren,
    // ... runs without a timer by reposting this handler at the end of the runnable
    // siehe: http://www.mopri.de/2010/timertask-bad-do-it-the-android-way-use-a-handler/
    //   und: https://stackoverflow.com/questions/4597690/how-to-set-timer-in-android
    //long startTime = 0;
    String subtTxtDisplayMirror = "";
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            int playerPosMs = 0;
            if(Data.mediaPlayer != null)
                playerPosMs = Data.mediaPlayer.getCurrentPosition();

            if(Data.SubtList != null) {
                //Log.i(TAG, "...Runable run() des Subtitle-Handlers");
                //int sMax = Data.SubtList.size() - 1;  // Wurde für Quick and Dirty Positions-Anzeige verwendet
                String toBeSubtTxt = "";
                int subtitleNeighbors[] = getActualSubtTxt(playerPosMs);
                int sId = subtitleNeighbors[0];
                if(sId >= 0) {
                    toBeSubtTxt = Data.SubtList.get(sId).Text;
                    currentTitle = sId;  // Nur wenn sId >= 0 diesen Wert speichern
                } else {
                    toBeSubtTxt = "";
                    currentTitle = 0;
                }
                //String toBeSubtTxt = "Pos=" + playerPosMs;  // Test
                if(toBeSubtTxt != subtTxtDisplayMirror && spIsPlaying == false) {
                    // Wenn Subtitel-Text neu, ihn im UI eintragen - wenn nicht Single Play
                    // des Titels: Das verhindert auf langsamen ePaper-Geräten unerw. Titelwechsel
                    subtTxtDisplayMirror = toBeSubtTxt;
                    tvSubtitle.setText(subtTxtDisplayMirror);
                    // Abspielposition im UI darstellen (z.B. 23%)
                    // ALT: Positionsanzeige anhand der Subtitle-Nr.
                    //if(sId >= 0) {
                    //    int posRatio = sId * 100 / sMax;
                    //    //tvTitlePosition.setText("" + posRatio + "%");
                    //    progressBar.setProgress(posRatio);
                    //}
                }
                previousTitle = subtitleNeighbors[1];
                nextTitle = subtitleNeighbors[2];
            }

            if(timerDeviderTmp >= timerDevider) {
                //playerPosMs für Progressbar verwenden - nicht so häufig wie Subtitle-Check
                if(playerPosMs >= 0 && duration100Ms > 0) {
                    int posTimeRatio = playerPosMs / duration100Ms + 1;
                    progressBar.setProgress(posTimeRatio);
                    //Log.i(TAG, "  Update ProgressBar %=" + posTimeRatio);
                }
                timerDeviderTmp = 0;
            } else {
                timerDeviderTmp++;
            }

            //tvSubtitle.setText(String.format("%d:%02d", minutes, seconds));  // Test
            timerHandler.postDelayed(this, 100);  // Wiederaufruf des Handlers nach delayMilliseconds
            //Log.i(TAG, " ----- timerHandler() wieder aufgerufen");
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Running onResume() ----- Erster bzw. erneuter Start des timerHandler()");

        // Erster (bzw. neuer - bei Rückkehr zur Activity) Start des Handlers zur periodischen Untertitel-Aktualisierung.
        //startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "Running onPause()");

        timerHandler.removeCallbacks(timerRunnable);

        // Die Anzeige der Subtitle-Anzeige wird mit onResume() wieder aufgenommen. Dort wird der
        // timerHandler also erneut gestartet.
        //
        // Übrigens: Das Abspiel im mediaPlayer geht bei ausgeschaltetem Bildschirm weiter. Dieses
        // Hintergrund-Spielen ist hier sehr wünschenswert! Auch in anderen Fällen! Daher wird
        // man den mediaPlayer hier im onPause nicht beenden. Beachte aber: Mache das
        // mediaPlayer-Objekt STATIC, damit beim onCreate nach einer Bildschirmrotation kein
        // weiteres mediaPlayer-Objekt hergestellt wird, das parallel zum vorhandenen Objekt spielt.

        // Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen ... hier nicht nötig?
        //playActualSubtitleTimerStop();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "Running onStop() --> stopping the Activity");
        // Das onStop() und onDestroy() läuft auch, wenn die Activity von einer anderen Activity der
        // App aus gestartet wurde und man per "Back-Button" zu dieser zurückkehrt. Und es läuft,
        // wenn man den Bildschim abschaltet (ON-Taste). In letzter Situation wollen wir nicht, das
        // der Player das Abspiel beendet. Daher beenden wir es hier Quick and Dirty im onBackPressed().
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "Running onStart() --> starting the Activity");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "  For the curious: Running onDestroy()");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // In diesem UI-Konzept: Bei Klick auf Back-Button soll das aktuelle Abspiel enden
        Log.i(TAG, "  For the curious: Running onBackPressed()");
        // Das ist ein Quick-And-Dirty-Moment, um einen im Hintergrund laufenden MediaPlayer zu
        // beenden. Er wird nicht im onStop() beendet, damit er bei dunklem Bildschirm weiterspielt.
        // Allerdings: onBackPressed() löst nicht aus, wenn Rückkehr per "upward navigation" im App-Bar erfolgt
        // Dafür das Release ZUSÄTZLICH auch in das onCreate der vorherigen Title-Activity programmieren!
        // (Sehr eigenartig: Nach onBackPressed() läuft das onCreate() in der Activity davor nicht!
        //  ERGO: Hardware Back-Button und dieser "upward navigation" Pfeil im App-Bar funktionieren verschieden!!!
        Data.mediaPlayer.release();  // Bringt das MediaPlayer-Objekt in den End-State -> Beendet Spiel
        Data.mediaPlayer = null;

        // Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
        playActualSubtitleTimerStop();
    }

    // Nun hier der übliche @Override der onCreate Methode
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Log.i(TAG, "Running onCreate()");

        // Show the actual album name on display
        tvAlbum = findViewById(R.id.tvAlbum);
        tvTitle = findViewById(R.id.tvTitle);
        tvAlbumTitle = findViewById(R.id.tvAlbumTitle);
        progressBar = findViewById(R.id.progressBar);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnNextTitle = findViewById(R.id.btnNextTitle);
        btnPreviousTitle = findViewById(R.id.btnPreviousTitle);
        btnNextSubt = findViewById(R.id.btnNextSubt);
        btnPreviousSubt = findViewById(R.id.btnPreviousSubt);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPlaySingle = findViewById(R.id.btnPlaySingle);

        btnNextTitle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoNextTitle();
            }
        });

        btnPreviousTitle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoPreviousTitle();
            }
        });

        btnNextSubt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoNextSubt();
            }
        });

        btnPreviousSubt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoPreviousSubt();
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doPlayPause();
            }
        });

        btnPlaySingle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Starte ein Single-Play des aktuellen Subtitles
                // Diese Methode beginnt das ein Single-Play nur, wenn der Player in Pause war.
                // Somit ist Mehrfach-Antippen dieses Buttons ungefährlich.
                playActualSubtitleButtonClick(currentTitle);
            }
        });

        //cntxt = this;                   // Diese Angabe des Context funktioniert.
        cntxt = getApplicationContext();  // Diese andere Angabe des Context funktioniert auch!

        // TitleList-Positions-Nummer erfahren
        int pos = Data.arrayListPosition;

        // Start playing, if the player isn't already playing after a screen rotation.
        if(Data.mediaPlayer == null) {
            // URI der Audiodatei mit Hilfe der TitleList-Positions-Nummer erfahren und abspielen
            playPositionTitle(pos);
            setPlayButtonIconColorToRed(true);  // Zu Beginn spielt der Player
        }

        // Anzeige von Album und Titel hier im onCreate für Situation "Orientierungswechsel"
        // Album-Name zeigen: Hier ist es immer derselbe Album-Name
        tvAlbum.setText(Data.storeAlbum);

        // Abzuspielenden Title anhand der TitleList-Positions-Nummer erfahren
        //String title = Data.storeTitle; // Alt - nutze den TitleList-Id!
        String title = Data.TitleList.get(pos).getTitle();
        // Titel-Namen zeigen
        tvTitle.setText(title);
        //// Abspiel-Position (z.B. 23%) schreiben. Hier ist sie unbekannt, daher Leerstring einsetzen
        //tvTitlePosition.setText("");
        // Progressbar auf 0% setzen
        progressBar.setProgress(0);
        // Anzeige der aktiellen Titelnummer innerhalb des Albums
        int nr = pos + 1;
        tvAlbumTitle.setText(nr + " / " + Data.TitleList.size());
    }

    void gotoNextTitle() {
        if(Data.arrayListPosition < Data.TitleList.size() - 1) {

            // Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
            playActualSubtitleTimerStop();

            Data.mediaPlayer.release();  // Bringt das MediaPlayer-Objekt in den End-State -> Beendet Spiel
            Data.mediaPlayer = null;

            Data.arrayListPosition++;
            int pos = Data.arrayListPosition;
            playPositionTitle(pos);
        }
    }

    void gotoPreviousTitle() {
        if(Data.arrayListPosition > 0) {

            // Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
            playActualSubtitleTimerStop();

            Data.mediaPlayer.release();  // Bringt das MediaPlayer-Objekt in den End-State -> Beendet Spiel
            Data.mediaPlayer = null;

            Data.arrayListPosition--;
            int pos = Data.arrayListPosition;
            playPositionTitle(pos);
        }
    }

    void gotoNextSubt() {
        //Log.i(TAG, "  btnNextSubt   Data.SubtList.size()=" + Data.SubtList.size());

        //// Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
        //playActualSubtitleTimerStop(); // ... Nun unnötig, weil Jumps während SP-Spiel verboten

        Log.i(TAG, "  gotoNextSubt (1)");
        if(Data.SubtList == null) Log.i(TAG, "  gotoNextSubt (2) LIST IS NULL");
        if(spIsPlaying == false) {
            // Navig.- Sprung im Titel, aber nicht während "SinglePlay"
            if(Data.SubtList != null && Data.SubtList.size() > 2 && nextTitle > 0) {    // Nimm ">2", sonst Crash ;)
                // Subtitleliste existiert und ist plausibel: Spiele den nächsten Subtitle
                Log.i(TAG, "  gotoNextSubt ==> NEXT ONE");
                int jumpToMs = Data.SubtList.get(nextTitle).StartMs;
                Data.mediaPlayer.seekTo(jumpToMs);
            }
            //else if(Data.SubtList.size() > 2  && currentTitle == Data.SubtList.size() - 1)
            //{
            //    // Zusatz-Angebot: Nach dem letzten Subtitle wechseln zu nächstem MP3-Title ... BUGGY?
            //    gotoNextTitle();
            //    Log.i(TAG, "  gotoNextSubt ==> gotoNextTitle");
            //}
            else
            {
                // Falls es keine Subtitel in dieser MP3 Datei gibt: Positioniere einfach um 10 Sekunden
                Log.i(TAG, "  gotoNextSubt ... + 10 Sek");
                Data.mediaPlayer.seekTo(Data.mediaPlayer.getCurrentPosition() + 10000); // +10 Sekunden
            }
        }
        timerDeviderTmp = timerDevider; // sofortige Fortschrittsbalken-Aktualisierung erreichen
    }

    void gotoPreviousSubt() {
        //Log.i(TAG, "  btnPreviousSubt   Data.SubtList.size()=" + Data.SubtList.size());

        //// Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
        //playActualSubtitleTimerStop(); // ...Nun unnötig, weil Jumps während SP-Spiel verboten

        Log.i(TAG, "  gotoPreviousSubt (1)");
        if(Data.SubtList == null) Log.i(TAG, "  gotoPreviousSubt (2) LIST IS NULL");

        if(spIsPlaying == false) {
            if(Data.SubtList != null && Data.SubtList.size() > 2) {    // Nimm ">2", sonst Crash ;)
                // Subtitleliste existiert und ist plausibel: Spiele den vorherigen Subtitle
                Log.i(TAG, "  gotoPreviousSubt ==> PREVIOUS ONE");
                int jumpToMs = Data.SubtList.get(previousTitle).StartMs;
                Data.mediaPlayer.seekTo(jumpToMs);
            }
            //else if(Data.SubtList.size() > 1 && currentTitle == 0) {
            //    // Zusatz-Angebot: Vor dem letzten Subtitle wechseln zu nächstem MP3-Title ... BUGGY?
            //    // ... funktioniert sowieso nicht.
            //    gotoPreviousTitle();
            //    Log.i(TAG, "  gotoPreviousSubt ==> gotoPreviousTitle");
            //}
            else
            {
                // Falls es keine Titel in dieser Datei gibt: Positioniere einfach um 10 Sekunden
                Log.i(TAG, "  gotoPreviousSubt ... - 10 Sek");
                Data.mediaPlayer.seekTo(Data.mediaPlayer.getCurrentPosition() - 10000);  // -10 Sekunden
            }
        }
        timerDeviderTmp = timerDevider; // sofortige Fortschrittsbalken-Aktualisierung erreichen
    }

    void doPlayPause() {
        // Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
        playActualSubtitleTimerStop();

        // Umschaltung Play - Pause
        if(Data.mediaPlayer.isPlaying()) {
            // Pausiere Spiel
            Data.mediaPlayer.pause();
            setPlayButtonIconColorToRed(false);  /// TODO: Diese Anzeige von Player-Events steuern!!!
            setSingleButtonIconColorToRed(false);
        }
        else {
            // Starte Spiel
            Data.mediaPlayer.start();
            setPlayButtonIconColorToRed(true);
        }
        timerDeviderTmp = timerDevider; // sofortige Fortschrittsbalken-Aktualisierung erreichen
    }



    void setVisibilitySpButton() {
        // Manage Sichtbarkeit Zusatz-Button "Spiele aktuellen Titel" ... TODO: Finde eine stabilere Lösung!
        if(Data.SubtList != null && Data.SubtList.size() > 2) {
            btnPlaySingle.setVisibility(VISIBLE);
            Log.i(TAG, "  in setVisibilitySpButton: btnPlaySingle Visibility = VISIBLE");
        } else {
            // Verberge "Play Single Title" Button
            btnPlaySingle.setVisibility(GONE);
            Log.i(TAG, "  in setVisibilitySpButton: btnPlaySingle Visibility = GONE");
        }
        spIsPlaying = false;  // Zu diesem Zeitpunkt hat noch niemand auf den SinglePlay Button geklickt
    }

    void playPositionTitle(int pos) {
        Log.i(TAG, "Running playPositionTitle(pos) ... pos=" + pos);

        // Anzeige der Titelnummer innherhalb des Albums
        int nr = pos + 1;
        String nrInfoString = nr + " / " + Data.TitleList.size();  // Beispiel:_ 5 / 12
        tvAlbumTitle.setText(nrInfoString);
        //// Abspielposition (z.B. 23%) schreiben. Hier: Am Anfang 0%
        //tvTitlePosition.setText("0%");
        //Progressbar auf 0% setzen
        progressBar.setProgress(0);

        // Abzuspielenden Title anhand der TitleList-Positions-Nummer erfahren
        //String title = Data.storeTitle; // Alt - nutze den TitleList-Id!
        String title = Data.TitleList.get(Data.arrayListPosition).getTitle();
        // Titel-Namen zeigen
        tvTitle.setText(title);

        // Subtitleliste aufbauen
        String subsText = Data.TitleList.get(Data.arrayListPosition).getComposer(); // darin ggf. SRT Untertitel
        //tvSubtitle.setText(subsText);  //Test
        prepareSubsDataSource(subsText);

        // Reset der Subtitle-Anzeige auf Standard-Text
        subtTxtDisplayMirror = nrInfoString;  // Wenn keine Subt. vorhanden, zeige andere Info
        //subtTxtDisplayMirror = nrInfoString + realComposer;  // "Zusätzlich Composer anzeigen" deaktiv.: ist nicht cool.
        tvSubtitle.setText(subtTxtDisplayMirror);

        // Info-Output der "üblichen" Metadaten aus dem TitleList Objekt
        Log.i(TAG, "Meta ...    Album=" + Data.TitleList.get(pos).getAlbum());
        Log.i(TAG, "Meta ...    Title=" + Data.TitleList.get(pos).getTitle());
        Log.i(TAG, "Meta ...   Artist=" + Data.TitleList.get(pos).getArtist());
        Log.i(TAG, "Meta ... Composer=" + Data.TitleList.get(pos).getComposer());

        // Get Title Uri from actual Element in TitleList
        Uri titleUri = Data.TitleList.get(pos).getTitleUri();

        // Play media (see: https://developer.android.com/guide/topics/media/mediaplayer)
        if(Data.mediaPlayer != null) {
            Data.mediaPlayer.release();
            Data.mediaPlayer = null;
        }
        Data.mediaPlayer = new MediaPlayer();
        // Event hinzufügen, der am Abspiel-Ende erlaubt, den Titel erneut zu spielen
        Data.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                timerDeviderTmp = timerDevider; // sofortige Fortschrittsbalken-Aktualisierung 100%
                setPlayButtonIconColorToRed(false);  // Hier genügt, die Icon-Farbe zu ändern
                setSingleButtonIconColorToRed(false);

                // Falls ein Single-Play des aktuellen Subtitles gestartet wurde, dieses verwerfen.
                playActualSubtitleTimerStop();
            }
        });
        try {
            //mp.setDataSource(titlePath);  // Do not use: Audio.Media.DATA is deprecated since Android 10
            Data.mediaPlayer.setDataSource(getApplicationContext(), titleUri);  // Use the title-URI instead
            Data.mediaPlayer.prepare();
            Data.mediaPlayer.start();
            setPlayButtonIconColorToRed(true);

            // Spieldauer des Titels ermitteln - für Funktion der Fortschrittsanzeige, Einheit 100ms
            duration100Ms = Data.mediaPlayer.getDuration() / 100;
            Log.i(TAG, "  duration100Ms=" + duration100Ms);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setVisibilitySpButton(); // Den SP Button nur zeigen, wenn es Untertitel gibt.

        /* Test periodische Textausgabe auf eInk Display
        // Einfacher Test der Bildschirm-Aktualisierung - für eInk-Displays
        if (tmr!= null) {
            tmr.cancel();
            tmr = null;
        }

        tmr = new CountDownTimer(31000, 1000) {

            public void onTick(long millisUntilFinished) {
                tvSubtitle.setText("countdown: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                tvSubtitle.setText("done!");
            }
        }.start();
        */
    }

    /**
     * Die Subtitleliste aufbauen, falls SRT formatierte Subtitle im übergebenen String gefunden werden
     * @param subsText SRT-Subtitle
     */
    void prepareSubsDataSource(String subsText) {
        // INFO: Wir benutzen dazu hier NICHT Androids SubtitleData, TimedText, etc., siehe:
        //   https://developer.android.com/reference/android/media/SubtitleData
        //   https://developer.android.com/reference/android/media/TimedText
        //   https://inducesmile.com/android-programming/how-to-get-videoview-subtitle-in-android/

        Log.i(TAG, "Running prepareSubsDataSource(subsText)");

        // Eine evtl. vom Vorgängertitel vorhandene Subtitel-Liste löschen
        if(Data.SubtList != null) {
            Data.SubtList.clear();
            Data.SubtList = null;
        }

        //realComposer = ""; // Variable für Anzeige des Composers in der Anzeige

        // Diese Methode beenden, wenn die Übergabe null ist (kann sein) oder keine Daten enthält
        // Wenn subsText SRT-Subtitle enthält, gibt es zu Beginn ein ":", siehe 00:00:00,000
        if(subsText == null || subsText.length() <= 20 || !subsText.substring(0,8).contains("00:")) {
            Log.i(TAG, "prepareSubsDataSource() --- No SubtitleData or composer found");
            return;  // Im Weiteren gilt für diesen Titel: realComposer = "";
        }
        //else if(subsText.substring(0,10).contains(":") == false) {   // Deaktiviert, da uncool
        //    Log.i(TAG, "prepareSubsDataSource() --- No SRT, but composer found");
        //    // Trage den Composer in Variable für Anzeige ein, mit Zeilenumbruch davor
        //   realComposer = System.getProperty("line.separator") + subsText;
        //    return;
        //}

        //tvSubtitle.setText(subsText);  //Test

        final String eol = System.getProperty("line.separator");  // Zeilenumbruch-Zeichen auf diesem System
        //final String eol = "\n";  // Zeilenumbruch-Zeichen Test

        // Zeilenweises Lesen, Einsortieren der Einträge in eine Liste.
        // Lies unbedingt //https://stackoverflow.com/questions/1096621/read-string-line-by-line
        String[] lines = subsText.split("\n");  // TODO: Fit machen für viele Zeilenumbruch-Varianten!
        Log.i(TAG, "SubtitleData Lines=" + lines.length);
        //tvSubtitle.setText("SubtitleData Lines=" + lines.length);  //Test
        String txt = "";
        String t1 = "";
        String t2 = "";
        Data.SubtList = new ArrayList<SubtLine>();
        boolean first = true;  // Kennzeichen, ob erster Untertitel erfasst ist
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            line.trim();   // TODO: Rauskriegen, ob es nur Spaces, oder alle Whitespaces am Rand entfernt

            //Log.i(TAG, "    Read=" + line);

            // Entdecken, ob die aktuelle Zeile die Zeiten so enthält: 00:00:35,800 --> 00:00:40,500
            // Verwende Regex - siehe:
            // https://code.tutsplus.com/tutorials/learn-java-for-android-development-string-basics--mobile-3824
            // https://stackoverflow.com/questions/6020384/create-array-of-regex-matches/46859130
            // https://stackoverflow.com/questions/9366742/android-regular-expression-return-matched-string
            Pattern p = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}.\\d{1,3}).*?(\\d{2}:\\d{2}:\\d{2}.\\d{1,3})");
            Matcher m = p.matcher(line);
            if(m.matches()) {
                t1 = m.group(1);
                t2 = m.group(2);
                txt = "";
                //Log.i(TAG, "    1. Match=" + t1);
                //Log.i(TAG, "    2. Match=" + t2);
            } else if(line.length() > 0) {
                txt = txt + line + eol;
                //Log.i(TAG, "    Append=" + line);
            } else {
                // Leere Zeile angetroffen: Text in die Liste eintragen
                //Log.i(TAG, "    Text Object Output=" + txt);

                // Trick, um später vor den ersten Titel navigieren zu können, davor einen Titel "" eintragen
                if (first && t1 != "00:00:00") {
                    SubtLine stObjStart = new SubtLine("00:00:00,000", t1, "");
                    Data.SubtList.add(stObjStart);
                    Log.i(TAG, "SubtitleData Zusatztitel am ANFANG zugef. linesLength=" + lines.length);
                }

                SubtLine stObj = new SubtLine(t1, t2, txt.trim());
                //Log.i(TAG, "     Element StartMs/Text="  + stObj.StartMs + "/" + stObj.Text);
                Data.SubtList.add(stObj);
                txt = "";  // Reset für neuen Texteintrag
                first = false;
            }
        }
        if(lines.length > 0) {
            // Falls allerletzter Texteintrag noch nicht in der Liste gespeichert ist, das nun tun
            if (txt != "") {
                SubtLine stObj = new SubtLine(t1, t2, txt.trim());
                Data.SubtList.add(stObj);
            }
            // Trick, um später nach den letzten Titel zu navigieren zu können, zum Schluss einen Titel eintragen
            SubtLine stObj = new SubtLine(t2, "29:59:59,999", "");
            Data.SubtList.add(stObj);
            Log.i(TAG, "SubtitleData Zusatztitel am ENDE zugef.");
        }

        // Mache den Zusatz-Button zum Single Title Spiel sichtbar ... TODO: Finde eine stabilere Lösung!
        if(Data.SubtList.size() > 2) {
            btnPlaySingle.setVisibility(VISIBLE);
            Log.i(TAG, "  in showSubtitles: btnPlaySingle Visibility = VISIBLE");
        } else {
            btnPlaySingle.setVisibility(GONE);
            Log.i(TAG, "  in showSubtitles: btnPlaySingle Visibility = GONE");
        }

        // Ein Timer ist schon gestartet - im onCreate. Zu jedem Timer-Tick wird geprüft, welcher Titel zu zeigen ist.
        subtTxtDisplayMirror = "";  // Ausgangszustand: Kein Text ist angezeigt.
        tvSubtitle.setText(subtTxtDisplayMirror);
    }

    /*
    String getActualSubtTxt(int playerPosMs) {
        // TODO: Diese Lösung optimieren!
        int pos = -1;  // Fundstellen-Position, -1 heißt, dass der Untertitel (noch) nicht gefunden ist
        int prevId = -1;
        int nexvId = -1;
        for (int i = 0; i < Data.SubtList.size(); i++) {
            if(playerPosMs > Data.SubtList.get(i).StartMs && playerPosMs < Data.SubtList.get(i).EndMs) {
                // Ein derzeit aktueller Text ist gefunden - diesen zurückgeben
                pos = i;
                break;
            }
        }
        if(pos >= 0) {
            return Data.SubtList.get(pos).Text;
        } else {
            // Wenn man hier landet, ist kein gerade geltender Text gefunden - also leeren String zurückgeben.
            return "";
        }
    }
    */

    /**
     * Id des zur übergebenen Abspiel-Position passenden Subtitles und dessen Nachbarn:
     * Subtitle-Id in [0] ist -1, wenn kein Text zur Position existiert. Sonst gibt es hier dessen Id.
     * Den Id des Vorgängertextes gibt es in [1], und den Id des Nachfolgertextes gibt es in [2].
     * Nachfolger-Id in [2] ist -1, wenn der letzte Subtitle schon erreicht ist.
     * Vorgänger-Id in [1] ist -1, wenn der erste Subzitle noch nicht erreicht war
     * Vorgänger-Id in [0] ist auch schon während des ersten Subtitles gleich 0.
     * Während aller folgenden Subtitle ist Vorgänger-Id in [0] um 1 kleiner als Subtitle-Id in [0].
     * VORAUSSETZUNG: Es gibt hier eine Liste der Subtitles: Data.SubtList
     * @param playerPosMs Array mit drei Id: [0] für Subtitle, [1] für Vorgänger, [2] für Nachfolger.
     * @return
     */
    int[] getActualSubtTxt(int playerPosMs) {
        // TODO: Diese Lösung prüfen: Ist sie korrekt, wenn der erste Untertitel spät kommt? Usw.
        // TODO: Eine Lösung finden, wenn keine SRT-Daten existieren, oder einzelne lange Zeiten darin vorkommen
        // TODO: Algorithmus dieser Lösung optimieren!
        int subtId = -1;  // Id des passenden Subtitles. -1 heißt, dass der Subtitle (noch) nicht gefunden ist
        int prevSubtId = -1;
        int nextSubtId = -1;
        int returnvalue[] = {-1, -1, -1};
        int SubtListLastIndex = Data.SubtList.size() - 1;
        //Log.i(TAG, "from getActualSubtTxt: SubtListLastIndex=" + SubtListLastIndex);
        int earlyestId = -1;
        int i;
        for (i = 0; i <= SubtListLastIndex; i++) {
            // Ermittle den frühesten Subtitle, dessen StartMs nach der Playerposition liegt.
            if(i < SubtListLastIndex) {
                if (playerPosMs >= Data.SubtList.get(i).StartMs && playerPosMs < Data.SubtList.get(i + 1).StartMs) {
                    earlyestId = i;
                    break;  // earlyestId ist ermittelt --> Verlasse die for-Schleife:
                }
            } else {
                if (playerPosMs >= Data.SubtList.get(i).StartMs) {
                    earlyestId = i;
                    break;  // earlyestId ist ermittelt --> Verlasse die for-Schleife
                }
            }
        }

        //Log.i(TAG, "from getActualSubtTxt: earlyestId=" + earlyestId);

        i = earlyestId;
        if(i >= 0) {
            // Wenn ein Titel gefunden: Stelle dann fest, ob die Anzeigedauer des Titels schon verstrichen ist.
            if (playerPosMs < Data.SubtList.get(i).EndMs) {
                subtId = i;
                prevSubtId = i - 1;
                if(prevSubtId < 0)
                    prevSubtId = 0;  // Am Listenanfang soll der Anfangs-Subtitle anstelle des Vorgänger-Subtitle spielen
                nextSubtId = i + 1;
                if(nextSubtId > SubtListLastIndex)
                    nextSubtId = -1;  // Am Listenende gibt es kein Nachfolger-Subtitle - Dann kein Spiel eines Nachfolgers
            } else {
                //Situation, dass die Darstellung des Subtitle-Textes schon beendet ist
                subtId = -1;      // Die Anzeigezeit des aktuellen Untertitels ist vorbei, also nichts anzeigen
                prevSubtId = i - 1;
                if(prevSubtId < 0)
                    prevSubtId = 0;  // Am Listenanfang soll der Anfangs-Subtitle anstelle des Vorgänger-Subtitle spielen
                nextSubtId = i + 1;
                if(nextSubtId > SubtListLastIndex)
                    nextSubtId = -1;  // Am Listenende gibt es kein Nachfolger-Subtitle - Dann kein Spiel eines Nachfolgers
            }
        } else {
            // Wenn erster Titel noch aussteht: Liefere nur den Index des ersten Titels (also 0) im Feld [2] "Nachfolger"
            nextSubtId = 0;
        }

        returnvalue[0] = subtId;  // Vor dem allerersten Subtitle steht hier ein -1 ... Keinen Untertitel anzeigen
        returnvalue[1] = prevSubtId;  // Ist 0 am Titel-Anfang
        returnvalue[2] = nextSubtId;  // Ist -1 am Titel-Ende --> Zum Navigatins-Teutpunkt prüfen und dann die Nav. nicht durchführen!

        //Log.i(TAG, "getActualSubtTxt returns [0]=" + returnvalue[0] + " [1]=" + returnvalue[1] + " [2]=" + returnvalue[2]);

        return returnvalue;
    }

    /**
     * Single-Playing des aktuellen Subtitles. Bedingungen dafür: Es war gerade Pause, und
     * der Titel-Eintrag existiert in der Subtitle-Liste.
     * @param currentTitle
     */
    void playActualSubtitleButtonClick(int currentTitle){
        Log.i(TAG, "Running playActualSubtitleButtonClick(n) ... n=" + currentTitle);

        if(!Data.mediaPlayer.isPlaying() && currentTitle >= 0
                && Data.SubtList != null && currentTitle < Data.SubtList.size()) {


            // TODO: Die Zeitsteuerung anders programmieren: Dafür nach Play-Start die Position
            //  abfragen und dann erst den Spieldauer-Timer stellen. Villeicht im OnSeekCompleteListener
            // Callbacks des MediaPlayer sind insgesamt:
            // OnPrepared, OnVideoSizeChangedListener, OnSeekCompleteListener, OnCompletion,
            // Hier die alte Lösung
            // Ermittle die Abspieldauer
            final int startMs = Data.SubtList.get(currentTitle).StartMs;
            final int endMs = Data.SubtList.get(currentTitle).EndMs;
            int durationMs = endMs - startMs - 5;

            // Wenn nennenswerte Abspieldauer vorliegt, diesen Subtitle spielen
            if (durationMs > 200) {
                // btnPlaySingle.setText("");  // TODO:Diesen windigen Code ersetzen
                Data.mediaPlayer.seekTo(startMs);
                Data.mediaPlayer.start();
                setPlayButtonIconColorToRed(true);
                setSingleButtonIconColorToRed(true);
                spIsPlaying = true;

                // TODO: Das hier in Event mediaPlayer.OnSeekCompleteListener tun - siehe oben.
                tmr = new CountDownTimer(durationMs, durationMs) {

                    public void onTick(long millisUntilFinished) {
                        // Not used.
                    }

                    public void onFinish() {
                        Data.mediaPlayer.pause();
                        Data.mediaPlayer.seekTo(endMs - 120);
                        spIsPlaying = false;
                        setPlayButtonIconColorToRed(false);
                        setSingleButtonIconColorToRed(false);
                        Log.i(TAG, " Single Subtitle Play done.");
                    }
                }.start();
            }
        }
    }

    /**
     * Ändern der Beschriftung des Play/Pause-Buttons
     * ... Das ist bei einem Icon-Button aber unmöglich! TODO: Das Play-Pause-Togglen anders lösen!
     * @param v
     */
    void setPlayButtonIconColorToRed(boolean v) {
        Log.i(TAG, " setPlayButtonIconColorToRed=" + v);
        if(v == true)
            btnPlayPause.setColorFilter(Color.argb(255, 255, 31, 0)); // Rot für "Abspiel läuft"
        else
            btnPlayPause.setColorFilter(Color.argb(0, 255, 255, 255)); // Weiß für Pause
    }

    void setSingleButtonIconColorToRed(boolean v) {
        Log.i(TAG, " setPlayButtonIconColorToRed=" + v);
        if(v == true)
            btnPlaySingle.setColorFilter(Color.argb(255, 255, 31, 0)); // Rot für "Abspiel läuft"
        else
            btnPlaySingle.setColorFilter(Color.argb(0, 255, 255, 255)); // Weiß für Pause
    }

    /**
     * Einen Auftrag, nur den aktuellen Subtitle zu spielen, wieder verwerfen
     */
    void playActualSubtitleTimerStop(){
        // Zertsöre den Timer, wenn er existieren sollte
        if (tmr!= null) {
            tmr.cancel();
            tmr = null;
            spIsPlaying = false;  // UI entsprechend updaten
            setSingleButtonIconColorToRed(false);
        }
        /*
        // Zeige/Verberge den "Play Single Title" Button, wenn Subtitles existieren
        if(Data.SubtList != null && Data.SubtList.size() > 0) {
            btnPlaySingle.setVisibility(VISIBLE);
            Log.i(TAG, "  in playActualSubtitleTimerStop: btnPlaySingle Visibility = VISIBLE");
        }
        */
    }
}


