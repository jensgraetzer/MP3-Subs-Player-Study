package com.example.a068mp3playerst;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

class TitleAdapter extends ArrayAdapter<Title> {
    LayoutInflater inflator;
    int itemLayout;
    // You must use EITHER android.R.layout.simple_list_item_1 OR ...simple_list_item_2
    public TitleAdapter(Context context,
                        int itemLayout,
                        List<Title> items) {
        super(context, itemLayout, items);    // We use the Adapter-Constructor with 3 parameters
        inflator = LayoutInflater.from(context);
        this.itemLayout = itemLayout;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflator.inflate (itemLayout, parent, false);
        }
        Title p = this.getItem(position);
        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        text1.setText(p.getTitle());
        if(itemLayout != android.R.layout.simple_list_item_1) {
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
            text2.setText(p.getArtist());
        }
        return convertView;
    }
}
