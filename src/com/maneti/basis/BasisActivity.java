package com.maneti.basis;

import java.io.File;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

public class BasisActivity extends Activity implements Runnable {
	public SharedPreferences prefs;
	private TextView myText = null;
	public ScrollView scrollView = null;
	public static BasisActivity instance;
	static String log = "Starting app:";
	BlueToothInterface BTInterface;
	public Handler updateHandler = new Handler();
	View logView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.activity_basis);
		logView = instance.findViewById(R.id.logView);
		File folder = new File(Environment.getExternalStorageDirectory() + "/basis");
		if (!folder.exists()) {
		    folder.mkdir();
		}
		BTInterface = new BlueToothInterface();		
		myText = new TextView(this);
		scrollView = new ScrollView(this);
		scrollView.addView(myText);
		myText.setText(log);
		setContentView(scrollView);
		//Decoder d = new Decoder("");
		//d.decodeFile(Environment.getExternalStorageDirectory() + "/basis/test.log");//only used for debugging
		
	}
	boolean inSettings = false;
	@Override
	public void onBackPressed() {
	    if (inSettings) {
	        backFromSettingsFragment();
	        return;
	    }
	    super.onBackPressed();
	}
	private void backFromSettingsFragment() {
	    inSettings = false;
	    getFragmentManager().popBackStack();
	}
	@Override
	public void run() {
		myText.setText(log);
		scrollView.scrollBy(0, scrollView.getHeight());
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_basis, menu);
		return true;
	}
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	        	case R.id.menu_settings:
	        		logView.setVisibility(View.GONE);
	        		getFragmentManager().beginTransaction()
	        			.replace(android.R.id.content, new SettingsFragment())
	        			.addToBackStack("settings")
	        			.commit();
	        		inSettings = true;
	        		break;
	        	case R.id.set_time:
	        		DialogFragment newFragment = new TimePickerFragment();
	      	        newFragment.show(getFragmentManager(), "timePicker");
	        		break;
	        	case R.id.pair:
	        		BTInterface.Pair();
	        		break;
	        	case R.id.force_erase:
	        		ForceErase();
	        		break;
	        }
	        return true;
	    }
	private void ForceErase(){
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	BasisActivity.instance.BTInterface.ForceDelete();
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            break; //do nothing
		        }
		    }
		};
		ab.setMessage("Are you sure?\nThis will delete all data on the watch even if it's not been downloaded yet").setPositiveButton("Yes", dialogClickListener)
	    	.setNegativeButton("No", dialogClickListener).show();

	}
	
	public static class SettingsFragment extends PreferenceFragment {
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View view = super.onCreateView(inflater, container, savedInstanceState);
	        view.setBackgroundColor(Color.WHITE);
	        view.setAlpha(1);
	        addPreferencesFromResource(R.xml.preferences);
	        return view;
	    }
	}
	public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current time as the default values for the picker
		final Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		
		// Create a new instance of TimePickerDialog and return it
		return new TimePickerDialog(getActivity(), this, hour, minute,
				DateFormat.is24HourFormat(getActivity()));
		}
		
		public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfHour) {
			hour = hourOfDay;
			minute = minuteOfHour;
			new DatePickerFragment().show(getFragmentManager(), "datePicker");
		}
	}
	static int hour = -1;
	static int minute = -2;
	public static class DatePickerFragment extends DialogFragment  implements DatePickerDialog.OnDateSetListener {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			
			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}
			
		public void onDateSet(DatePicker view, int year, int month, int day) {
			BasisActivity.instance.BTInterface.SetTime(year, month, day, hour, minute);
		}
	}
}
