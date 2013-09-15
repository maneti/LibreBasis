package com.maneti.basis;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;

public class Decoder {
	String raw = "";
	JSONObject currentMinute;
	public Decoder(String input){
		raw = input;
	}
	public void save(){
		JSONObject decoded = decode(raw.substring(6));
		Calendar cal = Calendar.getInstance();
		
		File file = new File(Environment.getExternalStorageDirectory() + "/basis/basis_log_chunk_num_"+Integer.parseInt(Utils.reverseBytes(raw.substring(2,6)), 16)+"_"+(cal.getTime().toString()).replace("/", ".")+".json");
		if (!file.exists()) {
	        try {
	            file.createNewFile();
	            FileWriter filewriter = new FileWriter(file,false);
	            filewriter.write(decoded.toString());
	            filewriter.flush();
	            filewriter.close();
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
	}
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    return sb.toString();
	}

	public static String getStringFromFile (String filePath) throws Exception {
	    File fl = new File(filePath);
	    FileInputStream fin = new FileInputStream(fl);
	    String ret = convertStreamToString(fin);
	    //Make sure you close all streams.
	    fin.close();        
	    return ret;
	}
	//debug method
	public void decodeFile(String filename){
		String s ="";
		try {
			s = getStringFromFile(filename);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		JSONObject decoded = decode(s.substring(6));
		Calendar cal = Calendar.getInstance();
		
		File file = new File(Environment.getExternalStorageDirectory() + "/basis/basis_log_chunk_num_"+Integer.parseInt(Utils.reverseBytes(s.substring(2,6)), 16)+"_"+(cal.getTime().toString()).replace("/", ".")+".json");
		if (!file.exists()) {
	        try {
	            file.createNewFile();
	            FileWriter filewriter = new FileWriter(file,false);
	            filewriter.write(decoded.toString());
	            filewriter.flush();
	            filewriter.close();
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
	}
	private JSONObject decode(String data){
		
		JSONObject root = new JSONObject();
		JSONObject heartRate = new JSONObject();
		JSONObject accelerometer = new JSONObject();
		try {
			root.put("heartRate", heartRate);
			root.put("accelerometer", accelerometer);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			String content = data;
			while (content.length()>920){
				content = ParseTime(accelerometer, content);
				content = ParseAccelerometer(currentMinute,content);
				int index = content.indexOf("2b00");
				ParseGalvanic(currentMinute,content);
				ParseTemperature(currentMinute, content);
				//minute['unknown_raw'] = content[0:index]
				content=content.substring(index);
				content=ParseTime(heartRate,content);
				content = ParseHeartRate(currentMinute,content);
			}
			Log.v("unpasred end of packet:",content);//need to deal with this later
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return root;
	}
	String ParseTime (JSONObject container, String data){
		int index = data.indexOf("0d05");
		String part = data.substring(0,index+4);
		String time = part.substring(index-4);
		int seconds = Integer.parseInt(Utils.reverseBytes(time), 16);
		Calendar cal = Calendar.getInstance();
		cal.set(2011, 0, 1);
		cal.add(Calendar.SECOND, seconds);
		String dateTime = cal.getTime().toString();
		if(!container.has(dateTime)){
			currentMinute = new JSONObject();
			try {
				container.put(dateTime, currentMinute);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				currentMinute = container.getJSONObject(dateTime);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return data.substring(index+4);
	}
	String ParseAccelerometer(JSONObject container, String data){
		String part = data.substring(0,540);
		JSONArray processed = new JSONArray();
		for (int i =0; i< part.length()-3; i+=3){
			JSONArray threeAxis = new JSONArray();
			threeAxis.put(Integer.parseInt(part.substring(i, i+1), 16));
			threeAxis.put(Integer.parseInt(part.substring(i+1, i+2), 16));
			threeAxis.put(Integer.parseInt(part.substring(i+2, i+3), 16));
			processed.put(threeAxis);
		}
		try {
			container.put("accelerometer", processed);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data.substring(540);
	}
	String ParseHeartRate(JSONObject container, String data){
		String part = data.substring(0,120);
		JSONArray processed = new JSONArray();
		for (int i =0; i< part.length()-2; i+=2){
			processed.put(Integer.parseInt(part.substring(i, i+2), 16));
				
		}
		try {
			container.put("heart_rate", processed);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data.substring(120);
	}

	String ParseTemperature(JSONObject container, String data){
		String part = data.substring(180, 204);
		JSONArray processed = new JSONArray();
		for (int i =0; i< part.length()-2; i+=2){
			processed.put(Integer.parseInt(part.substring(i, i+2), 16));
				
		}
		try {
			container.put("temperature", processed);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data.substring(204);
	}
	String ParseGalvanic(JSONObject container, String data){
		String part = data.substring(0,180);
		JSONArray processed = new JSONArray();
		for (int i =0; i< part.length()-6; i+=6){
			processed.put(Utils.reverseBytes(part.substring(i, i+6)));
				
		}
		try {
			container.put("galvanic_raw", processed);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data.substring(180);
	}
}
