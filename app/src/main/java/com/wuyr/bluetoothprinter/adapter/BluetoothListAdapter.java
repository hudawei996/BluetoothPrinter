package com.wuyr.bluetoothprinter.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wuyr.bluetoothprinter.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuyr on 17-7-26 下午1:15.
 */

public class BluetoothListAdapter extends RecyclerView.Adapter<BluetoothListAdapter.ViewHolder> {

    private int mLayoutId;
    private List<String> mData;
    private LayoutInflater mLayoutInflater;
    private OnItemClickListener mOnItemClickListener;

    public BluetoothListAdapter(Context context, int layoutId) {
        mLayoutId = layoutId;
        mData = new ArrayList<>();
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mLayoutInflater.inflate(mLayoutId, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.textView.setText(mData.get(position != 0 && position >= mData.size() ? mData.size() - 1 : position) == null ? "" : mData.get(position != 0 && position >= mData.size() ? mData.size() - 1 : position));
        if (mOnItemClickListener != null)
            holder.root.setOnClickListener(view -> mOnItemClickListener.onClick(mData.get(holder.getAdapterPosition() != 0 && holder.getAdapterPosition() >= mData.size() ? mData.size() - 1 : holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void add(String deviceName) {
        if (!mData.contains(deviceName)) {
            mData.add(deviceName);
            notifyDataSetChanged();
        }
    }

    public void remove(String deviceName) {
        if (mData.contains(deviceName)) {
            mData.remove(deviceName);
            notifyDataSetChanged();
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onClick(String deviceName);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        CardView root;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_view);
            root = itemView.findViewById(R.id.card_view);
        }
    }
}
