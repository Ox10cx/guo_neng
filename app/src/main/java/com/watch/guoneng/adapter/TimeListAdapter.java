package com.watch.guoneng.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.watch.guoneng.R;
import com.watch.guoneng.model.LightTimer;
import com.watch.guoneng.util.TimeUtil;

import java.util.ArrayList;

/**
 * Created by zsg on 2016/8/30.
 */
public class TimeListAdapter extends BaseAdapter {
    private Context context;
    public ArrayList<LightTimer> datas;

    public TimeListAdapter(Context context) {
        this.context = context;
        datas = new ArrayList<>();
    }

    /**
     * 更新数据
     *
     * @param lightTimers
     */
    public void update(ArrayList<LightTimer> lightTimers) {
        datas.clear();
        datas.addAll(lightTimers);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public Object getItem(int i) {
        return datas.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.light_time_item, null);
            viewHolder.tv_action = (TextView) convertView.findViewById(R.id.tv_action);
            viewHolder.tv_time = (TextView) convertView.findViewById(R.id.tv_time);
            viewHolder.tv_repeat = (TextView) convertView.findViewById(R.id.tv_repeat);
            viewHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        LightTimer lightTimer = datas.get(i);
        if (lightTimer.getAction().equals("on"))
            viewHolder.tv_action.setText(R.string.time_on);
        else
            viewHolder.tv_action.setText(R.string.time_off);

        viewHolder.tv_time.setText(String.format(context.getString(R.string.remain_time)
                , TimeUtil.getTimetoString(lightTimer.getDelay())));

        viewHolder.tv_repeat.setText(lightTimer.getRepeatToString());
        if (lightTimer.getName().trim().isEmpty())
            viewHolder.tv_name.setText("未命名");
        else
            viewHolder.tv_name.setText(lightTimer.getName());

        return convertView;
    }

    private final static class ViewHolder {
        public TextView tv_action;
        public TextView tv_time;
        public TextView tv_repeat;
        public TextView tv_name;
    }

}
