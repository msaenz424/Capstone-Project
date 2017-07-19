package com.android.mig.geodairy.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mig.geodairy.R;
import com.bumptech.glide.Glide;


public class GeodairyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final ImageView mPhoto;
    private final TextView mDate;

    public GeodairyViewHolder(View itemView) {
        super(itemView);
        mPhoto = (ImageView) itemView.findViewById(R.id.photo_image_view);
        mDate = (TextView) itemView.findViewById(R.id.date_text_view);
    }

    public void setDate(String date){
        mDate.setText(date);
    }

    public void setPhoto(String photoUrl){
        Glide.with(mPhoto.getContext())
                .load(photoUrl)
                .into(mPhoto);
    }

    @Override
    public void onClick(View view) {

    }
}
