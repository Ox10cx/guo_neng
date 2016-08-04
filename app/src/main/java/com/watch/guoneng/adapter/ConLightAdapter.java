package com.watch.guoneng.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.watch.guoneng.R;
import com.watch.guoneng.model.Light;
import com.watch.guoneng.ui.AddLightActivity;

import java.util.LinkedList;
import java.util.List;

public class ConLightAdapter extends BaseExpandableListAdapter {
    private static String TAG = "ConLightAdapter";
    private LayoutInflater inflater;
    private LinkedList<String> fatherList;
    private List<LinkedList<Light>> childList;
    private Context mcontext;
    private ExpandableListView expandableListView;

    public ConLightAdapter(LinkedList<String> fatherList, List<LinkedList<Light>> childList, Context mcontext) {
        this.fatherList = fatherList;
        this.childList = childList;
        Log.i(TAG, "childList.size:" + this.childList.size());
        this.mcontext = mcontext;
        inflater = LayoutInflater.from(mcontext);
    }

    public ConLightAdapter(LinkedList<String> fatherList, List<LinkedList<Light>> childList, Context mcontext, ExpandableListView expandableListView) {
        this(fatherList, childList, mcontext);
        this.expandableListView = expandableListView;
    }

    // 返回父列表个数
    @Override
    public int getGroupCount() {
        return fatherList.size();
    }


    class GroupHolder {
        TextView group_name;
        View group_line1;
        ImageView group_iv;
        ImageView iv_addlight;
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
        if (childList.size() == 0) {
            return 0;
        }
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
            groupHolder.group_iv = (ImageView) convertView.findViewById(R.id.iv_isspread);
            groupHolder.iv_addlight = (ImageView) convertView.findViewById(R.id.iv_addlight);
            groupHolder.iv_addlight.setOnClickListener(new MyOnClickListener(groupPosition));
            convertView.setTag(groupHolder);
        } else {
            groupHolder = (GroupHolder) convertView.getTag();
        }

//        if (groupPosition == 0) {
//            groupHolder.group_line1.setVisibility(View.GONE);
//        } else {
//            groupHolder.group_line1.setVisibility(View.VISIBLE);
//        }
        groupHolder.group_name
                .setText(fatherList.get(groupPosition));
        if (expandableListView.isGroupExpanded(groupPosition)) {
            groupHolder.group_iv.setImageResource(R.drawable.below_go);
        } else {
            groupHolder.group_iv.setImageResource(R.drawable.right_go);
        }
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

        childHolder.child_name.setText(mcontext.getString(R.string.light_name) + childList.get(groupPosition).get(childPosition).getName());
        childHolder.light_no.setText(mcontext.getString(R.string.light_no) + childList.get(groupPosition).get(childPosition).getId());
        if (childList.get(groupPosition).get(childPosition).getLightStatu()==3) {
            childHolder.light_of_on.setImageResource(R.drawable.toolbar_lamp_on);
        } else {
            childHolder.light_of_on.setImageResource(R.drawable.toolbar_lamp_off);
        }
//        if (isLastChild)
//            childHolder.child_line.setVisibility(View.GONE);
//        else {
//            childHolder.child_line.setVisibility(View.VISIBLE);
//        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private class MyOnClickListener implements View.OnClickListener {
        private int index;
        private Intent intent;
        private int default_no = 1;

        public MyOnClickListener(int index) {
            this.index = index;
            Log.i(TAG, "index:" + this.index + "   default_no:" + default_no);
        }

        @Override
        public void onClick(View view) {
            intent = new Intent(mcontext, AddLightActivity.class);
            intent.putExtra("group_index", index);
            intent.putExtra("type","addlight");
            if (childList.size() != 0) {
                default_no = childList.get(index).size() + 1;
                Log.i(TAG, "index:" + index + "   default_no:" + default_no);
            }
            intent.putExtra("default_no", default_no);
            ((Activity) mcontext).startActivityForResult(intent, 111);
        }
    }

}
