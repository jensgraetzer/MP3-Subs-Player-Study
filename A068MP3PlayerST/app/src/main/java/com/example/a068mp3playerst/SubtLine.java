package com.example.a068mp3playerst;

public class SubtLine {
    int StartMs;   // MediaPlayer uses Milliseconds in Methods getCurrentPosition(), seekTo(), ...
    int EndMs;
    String Text;

    SubtLine(String Start, String End, String Text) {
        StartMs = Tools.convertDurationStringToMs(Start);
        EndMs = Tools.convertDurationStringToMs(End);
        this.Text = Text;
    }

    SubtLine(int StartMs, int EndMs, String Text) {
        this.StartMs = StartMs;
        this.EndMs = EndMs;
        this.Text = Text;
    }
}
