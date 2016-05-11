package com.watch.wifidemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import com.watch.wifidemo.R;
import com.watch.wifidemo.adapter.ConLightAdapter;
import com.watch.wifidemo.model.Light;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 16-3-7.
 */
public class GroupLightActivity extends BaseActivity implements ExpandableListView.OnChildClickListener {
    private ExpandableListView expandableListView;
    private LinkedList<String> fatherList;
    private List<LinkedList<Light>> childList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grouplight);
        expandableListView = (ExpandableListView) findViewById(R.id.scrolllistview);
        expandableListView.setOnChildClickListener(this);
        initData();
    }

    public void initData() {
        fatherList = new LinkedList<String>();
        fatherList.add(getResources().getString(R.string.use_light));
        fatherList.add(getResources().getString(R.string.useless_light));
        fatherList.add(getResources().getString(R.string.other_light));
        childList = new LinkedList<LinkedList<Light>>();
        int count = 0;
        LinkedList<Light> temlist;
        for (int i = 0; i < fatherList.size(); i++) {
            temlist = new LinkedList<Light>();
            for (int j = 0; j < 5; j++) {
                Light light = new Light();
                light.setId("" + count);
                light.setName("第" + (i + 1) + "组" + "第" + (j + 1) + "个彩灯");
                temlist.add(light);
                count++;
            }
            childList.add(temlist);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        ConLightAdapter adapter = new ConLightAdapter(fatherList, childList, this);
        expandableListView.setAdapter(adapter);
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int gid, int cid, long l) {
        Light light = childList.get(gid).get(cid);
        Intent intent = new Intent(this, ControlLightActivity.class);
        intent.putExtra("light", light);
        startActivity(intent);
        return false;
    }

}
