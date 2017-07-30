package com.android.mig.geodiary.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mig.geodiary.R;
import com.bumptech.glide.Glide;


public class GeoDiaryViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mPhoto;
    private final TextView mDate;

    public GeoDiaryViewHolder(View itemView) {
        super(itemView);
        mPhoto = (ImageView) itemView.findViewById(R.id.photo_image_view);
        mDate = (TextView) itemView.findViewById(R.id.date_text_view);
        //itemView.setOnClickListener();
    }

    public void setDate(String date){
        mDate.setText(date);
    }

    public void setPhoto(String photoUrl){
        Glide.with(mPhoto.getContext())
                .load(photoUrl)
                .into(mPhoto);
    }
}
