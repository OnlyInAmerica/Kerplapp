
package net.binaryparadox.kerplapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

public class AppListAdapter extends ArrayAdapter<AppListEntry> {
    public ArrayList<AppListEntry> appList;
    private Activity activity;

    public AppListAdapter(Activity activity, Context context,
            int textViewResourceId, ArrayList<AppListEntry> appList) {
        super(context, textViewResourceId, appList);
        this.appList = new ArrayList<AppListEntry>();
        this.appList.addAll(appList);
        this.activity = activity;
    }

    private class ViewHolder {
        TextView appName;
        CheckBox appPkg;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Log.v("ConvertView", String.valueOf(position));

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.app_select_info, null);

            holder = new ViewHolder();
            holder.appName = (TextView) convertView.findViewById(R.id.appName);
            holder.appPkg = (CheckBox) convertView.findViewById(R.id.appCheckbox);
            convertView.setTag(holder);

            holder.appPkg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    AppListEntry app = (AppListEntry) cb.getTag();
                    app.setChecked(cb.isChecked());

                    /*
                     * Toast.makeText(ctx, "Clicked on Checkbox: " +
                     * cb.getText() + " is " + cb.isChecked(),
                     * Toast.LENGTH_LONG).show();
                     */
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppListEntry app = appList.get(position);
        holder.appName.setText(app.getAppName());
        holder.appPkg.setText(app.getPkgName());
        holder.appPkg.setTag(app);
        holder.appPkg.setChecked(app.isChecked());

        return convertView;
    }
}
