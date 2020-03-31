package com.example.a068mp3playerst;

import android.net.Uri;

public class Title {
    Long Id;     // _ID in the MediaStore
    String Title = "";
    String Album = "";
    String Artist = "";
    String Track = "";  // Title-Number within an album
    String DisplayName = "";
    String Composer = "";
    Uri TitleUri = null;  // the mediaplayer plays this uri

    public Long getId() {
        return Id;
    }
    public void setId(Long Id) {
        this.Id = Id;
    }

    public String getTitle() {
        return Title;
    }
    public void setTitle(String Title) {
        this.Title = Title;
    }

    public String getAlbum() {
        return Album;
    }
    public void setAlbum(String Album) {
        this.Album = Album;
    }

    public String getArtist() {
        return Artist;
    }
    public void setArtist(String Artist) {
        this.Artist = Artist;
    }

    public String getTrack() {
        return Track;
    }
    public void setTrack(String Track) {
        this.Track = Track;
    }

    public Uri getTitleUri() {
        return TitleUri;
    }
    public void setTitleUri(Uri TitleUri) {
        this.TitleUri = TitleUri;
    }

    public String getDisplayName() {
        return DisplayName;
    }
    public void setDisplayName(String DisplayName) {
        this.DisplayName = DisplayName;
    }

    public String getComposer() {
        return Composer;
    }
    public void setComposer(String Composer) {
        this.Composer = Composer;
    }
}

