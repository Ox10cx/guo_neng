package com.watch.wifidemo.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.watch.wifidemo.R;
import com.watch.wifidemo.model.WifiDevice;
import com.watch.wifidemo.util.CommonUtil;
import com.watch.wifidemo.util.ImageLoaderUtil;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListAdapter extends BaseAdapter {
    private static final long ANIMATION_DURATION = 300;
    private Context context;
    private ArrayList<WifiDevice> data;
    private int mId;
    OnItemClickCallback mCallback;

    public DeviceListAdapter(Context context, ArrayList<WifiDevice> list, OnItemClickCallback listener) {
        this.context = context;
        data = list;
        mId = 0;
        mCallback = listener;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public int getmId() {
        return mId;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holderView;

        if (convertView == null) {
            holderView = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.device_item, null);
            holderView.image = (ImageView) convertView.findViewById(R.id.device_image);
            holderView.name = (TextView) convertView.findViewById(R.id.device_name);

            holderView.status = (TextView) convertView.findViewById(R.id.device_status);

            //holderView.button = (Button) convertView.findViewById(R.id.device_button);

            holderView.right_arrow = (ImageView) convertView.findViewById(R.id.right_arrow);

            convertView.setTag(holderView);
        } else {
            holderView = (ViewHolder) convertView.getTag();
        }

        String path = CommonUtil.getImageFilePath(data.get(position).getThumbnail());
        // Log.e("hjq", "path = " + path);
        if (path != null) {
            ImageLoaderUtil.displayImage("file://" + path, holderView.image, context);
        } else {
            Drawable d = context.getResources().getDrawable(R.drawable.wifi_device);
            holderView.image.setImageDrawable(d);
        }

        holderView.name.setText(data.get(position).getName());

        int status = data.get(position).getStatus();
        Log.d("hjq", "postion " + position + " status = " + status);
        if (status == WifiDevice.INACTIVE_STATUS) {
            holderView.status.setText(R.string.str_inactive);
        } else if (status == WifiDevice.LOGIN_STATUS) {
            holderView.status.setText(R.string.str_online);
        } else if (status == WifiDevice.LOGOUT_STATUS) {
            holderView.status.setText(R.string.str_offline);
        } else {
            holderView.status.setText(R.string.str_unkown);
        }


        holderView.right_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mId = position;
                mCallback.onRightArrowClick(position);
            }
        });

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void updateDataSet(int position) {
        Log.i("hjq", "update position =" + position);
        data.remove(position);
        notifyDataSetChanged();
    }

    private final static class ViewHolder {
        public ImageView image;
        public TextView name;
        public TextView status;
        //public Button button;
        public ImageView right_arrow;
    }

    public interface OnItemClickCallback {
        void onButtonClick(View v, int position);

        void onRightArrowClick(int postion);
    }

}
