package com.watch.wifidemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.watch.wifidemo.R;
import com.watch.wifidemo.model.Light;

import java.util.LinkedList;
import java.util.List;

public class ConLightAdapter extends BaseExpandableListAdapter {
    private LayoutInflater inflater;
    private LinkedList<String> fatherList;
    private List<LinkedList<Light>> childList;
    private Context mcontext;

    public ConLightAdapter(LinkedList<String> fatherList, List<LinkedList<Light>> childList, Context mcontext) {
        this.fatherList = fatherList;
        this.childList = childList;
        this.mcontext = mcontext;
        inflater = LayoutInflater.from(mcontext);
    }

    // 返回父列表个数
    @Override
    public int getGroupCount() {
        return fatherList.size();
    }



    class GroupHolder {
        TextView group_name;
        View group_line1;
    }

    class ChildHolder {
        TextView child_name;
        TextView light_no;
        ImageView light_of_on;
        View child_line;
    }

    // 返回子列表个数
    @Override
    public int getChildrenCount(int groupPosition) {
        return childList.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return fatherList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return (childList.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {

        return true;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        GroupHolder groupHolder = null;
        if (convertView == null) {
            groupHolder = new GroupHolder();
            convertView = inflater.inflate(R.layout.con_light_group_item,
                    null);
            groupHolder.group_name = (TextView) convertView
                    .findViewById(R.id.group_name);
            groupHolder.group_line1 = (View) convertView
                    .findViewById(R.id.group_line1);
            convertView.setTag(groupHolder);
        } else {
            groupHolder = (GroupHolder) convertView.getTag();
        }

        if (groupPosition == 0) {
            groupHolder.group_line1.setVisibility(View.GONE);
        } else {
            groupHolder.group_line1.setVisibility(View.VISIBLE);
        }
        groupHolder.group_name
                .setText(fatherList.get(groupPosition));
        return convertView;
    }


    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        ChildHolder childHolder = null;
        if (convertView == null) {
            childHolder = new ChildHolder();
            convertView = inflater.inflate(R.layout.con_light_child_item,
                    null);
            childHolder.child_name = (TextView) convertView
                    .findViewById(R.id.child_name);
            childHolder.light_no = (TextView) convertView
                    .findViewById(R.id.light_no);
            childHolder.light_of_on = (ImageView) convertView
                    .findViewById(R.id.light_of_on);
            childHolder.child_line = convertView.findViewById(R.id.child_line);
            convertView.setTag(childHolder);
        } else {
            childHolder = (ChildHolder) convertView.getTag();
        }

        childHolder.child_name.setText(childList.get(groupPosition).get(childPosition).getName());
        childHolder.light_no.setText("彩灯编号：" + childList.get(groupPosition).get(childPosition).getId());
        if (childList.get(groupPosition).get(childPosition).is_on()) {
            childHolder.light_of_on.setImageResource(R.drawable.toolbar_lamp_on);
        } else {
            childHolder.light_of_on.setImageResource(R.drawable.toolbar_lamp_off);
        }
        if (isLastChild)
            childHolder.child_line.setVisibility(View.GONE);
        else {
            childHolder.child_line.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


}
