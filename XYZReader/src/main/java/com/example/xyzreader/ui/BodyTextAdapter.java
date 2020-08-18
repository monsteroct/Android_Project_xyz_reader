package com.example.xyzreader.ui;

import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;

import java.util.List;

/**
 * A RecyclerView Adapter which follows the long text best practice in 2018 I/O to improve
 * performance issue dealing with long styled text.
 */
public class BodyTextAdapter extends RecyclerView.Adapter<BodyTextAdapter.TextViewHolder> {

    public static final String TAG = "BodyTextAdapter";
    private List<Spannable> spannableList;

    public BodyTextAdapter(List<Spannable> spannableList) {
        this.spannableList = spannableList;
    }

    @Override
    public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.detail_content_piece, parent, false);
        return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TextViewHolder holder, int position) {
        // must use type SPANNABLE, otherwise it will create a copy of immutable Spanned object
        holder.textPiece.setText(spannableList.get(position), TextView.BufferType.SPANNABLE);
    }

    @Override
    public int getItemCount() {
        return spannableList == null ? 0 : spannableList.size();
    }

    public void setSpannableList(List<Spannable> spannableList) {
        // allows to update data asynchronously
        this.spannableList = spannableList;
        notifyDataSetChanged();
    }

    class TextViewHolder extends RecyclerView.ViewHolder{

        TextView textPiece;

        public TextViewHolder(View itemView) {
            super(itemView);
            textPiece = itemView.findViewById(R.id.tv_content_piece);

            textPiece.setSpannableFactory(new Spannable.Factory(){
                // if TextView.setText() BufferType is SPANNABLE
                // by default it will create a COPY (call setSpannableFactory.newSpannable())
                // of the original text in form of Spannable.
                // In order to reuse and suppress object creation, we could maintain the
                // list of spannable ourselves and override the method.
                // https://developer.android.com/guide/topics/text/spans#create-and-apply
                @Override
                public Spannable newSpannable(CharSequence source) {
                    return (Spannable) source;
                }
            });
        }
    }

}
