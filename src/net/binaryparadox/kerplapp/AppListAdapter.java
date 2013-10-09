package net.binaryparadox.kerplapp;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import net.binaryparadox.kerplapp.KerplappRepo.App;

public class AppListAdapter extends ArrayAdapter<App>
{
  public  ArrayList<App> appList;
  private Activity       activity;

  public AppListAdapter(Activity activity, Context context, 
                        int textViewResourceId, ArrayList<App> appList)
  {
    super(context, textViewResourceId, appList);
    this.appList = new ArrayList<App>();
    this.appList.addAll(appList);
    this.activity = activity;
  }

  private class ViewHolder
  {
    TextView appName;
    CheckBox appPkg;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent)
  {
    ViewHolder holder = null;
    Log.v("ConvertView", String.valueOf(position));

    if (convertView == null)
    {
      LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      convertView = vi.inflate(R.layout.app_select_info, null);

      holder = new ViewHolder();
      holder.appName = (TextView) convertView.findViewById(R.id.appName);
      holder.appPkg  = (CheckBox) convertView.findViewById(R.id.appCheckbox);
      convertView.setTag(holder);

      holder.appPkg.setOnClickListener(new View.OnClickListener() 
      {
        public void onClick(View v)
        {
          CheckBox cb = (CheckBox) v;
          App app = (App) cb.getTag();
          /*
          Toast.makeText(ctx, "Clicked on Checkbox: " + cb.getText() + " is " + cb.isChecked(),
              Toast.LENGTH_LONG).show();
          */
          app.includeInRepo = cb.isChecked();
        }
      });
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    App app = appList.get(position);
    holder.appName.setText(app.name);
    holder.appPkg.setText(app.id);
    holder.appPkg.setTag(app);
    holder.appPkg.setChecked(app.includeInRepo);

    return convertView;
  }
}