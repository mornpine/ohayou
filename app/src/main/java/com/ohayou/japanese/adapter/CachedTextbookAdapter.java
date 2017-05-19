package com.ohayou.japanese.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ohayou.japanese.OhayouApplication;
import com.ohayou.japanese.R;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.Textbook;
import com.ohayou.japanese.utils.CommUtils;

import java.io.File;
import java.util.List;

/**
 * Created by Oxygen on 16/1/7.
 */
public class CachedTextbookAdapter extends BaseAdapter implements View.OnClickListener{
    Context mContext;
    List<Textbook> mData;

    public CachedTextbookAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.item_cached_textbook, null);
            holder = new ViewHolder();
            holder.textbook = (TextView) convertView.findViewById(R.id.textbook);
            holder.delete = convertView.findViewById(R.id.delete);
            holder.delete.setOnClickListener(this);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        Textbook textbook = mData.get(position);
        holder.textbook.setText(String.format("%s (%s)", textbook.name, textbook.getTextbookSize()));
        holder.delete.setTag(position);

        return convertView;
    }

    static class ViewHolder {
        TextView textbook;
        View delete;
    }

    public void setData(List<Textbook> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        Textbook textbook = mData.get(position);
        File dir = textbook.getTextbookDir();
        CommUtils.deleteFiles(dir);
        dir.delete();
        mData.remove(position);
        DatabaseHelper helper = ((OhayouApplication)mContext.getApplicationContext()).getDatabaseHelper();
        helper.deleteTextbooks(textbook);
        notifyDataSetChanged();
    }
}
