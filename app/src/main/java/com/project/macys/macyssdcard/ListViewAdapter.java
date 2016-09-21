package com.project.macys.macyssdcard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Anusha on 9/18/2016.
 */
public class ListViewAdapter extends ArrayAdapter<String>{


    Context context;
    int layoutResource;
    ArrayList<String> list;
    ViewHolder viewHolder;

    public ListViewAdapter(Context context, int layoutResource, ArrayList<String> list){
        super(context,layoutResource,list);
        this.context = context;
        this.layoutResource = layoutResource;
        this.list = list;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(convertView == null){
            convertView = layoutInflater.inflate(layoutResource, null, true);

            viewHolder = new ViewHolder();

                viewHolder.fileName = (TextView) convertView.findViewById(R.id.statistic_type);
                viewHolder.statType = (TextView) convertView.findViewById(R.id.statistic_value);

                convertView.setTag(viewHolder);

        }
        else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String[] split = list.get(position).split(";");
        viewHolder.fileName.setText(split[0]);
        viewHolder.statType.setText(split[1]);

        return convertView;
    }


    private static class ViewHolder{
        public TextView fileName;
        public TextView statType;
    }
}
