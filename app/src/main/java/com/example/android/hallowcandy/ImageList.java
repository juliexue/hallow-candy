package com.example.android.hallowcandy;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

public class ImageList extends ArrayAdapter<JSONObject>{
    private final Activity context;
    List<JSONObject> images;

    public ImageList(Activity context, List<JSONObject> images) {
        super(context, R.layout.list_item, images);
        this.images = images;
        this.context = context;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.list_item, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
        try{
            txtTitle.setText(images.get(position).get("lat").toString());
        } catch (Exception e) {
            txtTitle.setText("woops");
        }
        //set image please
        return rowView;
    }
}