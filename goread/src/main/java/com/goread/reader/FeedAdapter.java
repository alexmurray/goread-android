package com.goread.reader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class FeedAdapter extends ArrayAdapter<Outline> {

    private final Context context;
    private final int rowResourceId;

    public FeedAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);

        this.context = context;
        this.rowResourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(rowResourceId, parent, false);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
        TextView textView = (TextView) rowView.findViewById(R.id.textView);
        Outline o = getItem(position);
        textView.setText(o.Title);
        if (o.Icon == MainActivity.ICON_FOLDER) {
            imageView.setImageResource(R.drawable.ic_folder_close);
        } else if (o.Icon != null) {
            Picasso.with(context).load(o.Icon).into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_icon_grey);
        }
        return rowView;
    }
}