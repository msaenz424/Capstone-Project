package com.android.mig.geodiary.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mig.geodiary.QuoteHandler;
import com.android.mig.geodiary.R;

import java.util.ArrayList;

public class QuotesAdapter extends RecyclerView.Adapter<QuotesAdapter.QuotesAdapterViewHolder>{

    private QuoteHandler mQuoteHandler;
    private ArrayList<String> mQuotesArrayList;

    public QuotesAdapter(QuoteHandler quoteHandler){
        mQuoteHandler = quoteHandler;
        mQuotesArrayList = new ArrayList<>();
    }

    public void setQuotesAdapter(ArrayList<String> quotesArray){
        mQuotesArrayList = quotesArray;
        notifyDataSetChanged();
    }

    @Override
    public QuotesAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_quote, parent, false);
        return new QuotesAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final QuotesAdapterViewHolder holder, int position) {
        String quote = mQuotesArrayList.get(position);
        holder.mTextViewQuote.setText("\"" + quote + "\"");
    }

    @Override
    public int getItemCount() {
        return mQuotesArrayList.size();
    }

    public class QuotesAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView mTextViewQuote;
        ImageView mImageViewCopy;

        public QuotesAdapterViewHolder(View itemView) {
            super(itemView);
            mTextViewQuote = (TextView) itemView.findViewById(R.id.item_quote_text_view);
            mImageViewCopy = (ImageView) itemView.findViewById(R.id.copy_image_view);
            mImageViewCopy.setOnClickListener(this);
            mImageViewCopy.setFocusable(true);
        }

        @Override
        public void onClick(View view) {
            String quote = mQuotesArrayList.get(getAdapterPosition());
            mQuoteHandler.OnClick(quote);
        }
    }
}
