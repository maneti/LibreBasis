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
		min.set(2011, 0, 1);
		max.set(2050, 0, 1);

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
		min.set(2011, 0, 1);
		max.set(2050, 0, 1);
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
	private JSONObject decode(String contents){
		
		JSONObject root = new JSONObject();
		String extra = "";
		Calendar time = Calendar.getInstance();;
		JSONObject timeContainer= new JSONObject();
		while (contents.contains("2900")){
			
			String chunk = contents.substring(0,contents.indexOf("2900")+4);
			contents = contents.substring(contents.indexOf("2900")+4);
			try {
				int timeLength = 32;
				if  (firstDate == null && contents.indexOf("2900")>0){
					ParseTime(contents.substring(0, 8));
				}
				while (!isValidTime(contents.substring(0,timeLength))){
					chunk = chunk+contents.substring(0,contents.indexOf("2900")+4);
					contents = contents.substring(contents.indexOf("2900")+4);
				}
				PasredDate result = ParseTime(chunk.substring(0, timeLength));
				extra = result.extraBytes;
				time = result.date;
				
				if (time == null){
					//print 'bad time: '+chunk[0:timeLength]
					//print chunk
					continue;//corrupt(?) block without time, skip
					//break
				}
				chunk = chunk.substring(0,chunk.length()-4);
				if (chunk.length() < 80){
					//print 'skipping:'+chunk
					continue;//sanity check: not sure what happened but chunks must be longer
				}
				chunk=extra+chunk.substring(timeLength);//#time
				timeContainer = new JSONObject();
					
				if(root.has(time.getTime().toString())){
					timeContainer = root.getJSONObject(time.getTime().toString());
				}
				
				root.put(time.getTime().toString(), timeContainer);
				boolean partChunk = chunk.length() < 400;

				String heartData = chunk.substring(chunk.length()-120);
				if (!isValidTime(chunk.substring(chunk.length()-128,chunk.length()-120))){
					heartData = chunk.substring(chunk.length()-122,chunk.length()-2);
					chunk=chunk.substring(0,chunk.length()-2);
				}
				timeContainer.put("heart rate", ParseHeartRate(heartData));
				chunk = chunk.substring(0,chunk.length()-120);//#heart rate
				chunk = chunk.substring(0,chunk.lastIndexOf("2b00"));//#another datetime, not sure why
				if (partChunk){//#this chunk only has heart rate, temp and galvanic data
					if (chunk.length() > 150){
						chunk = chunk.substring(0,chunk.lastIndexOf("3c00"));//#also sometimes extra unknown minichunk
						chunk = chunk.substring(0,chunk.length()-12);//#another datetime, not sure why
					}
				} else{// #this has a full data set
					chunk = chunk.substring(0,chunk.length()-12);//#unknown
				}
				String tempData = chunk.substring(chunk.length()-16);
				timeContainer.put("temperature", ParseHeartRate(tempData));
				chunk = chunk.substring(0,chunk.length()-16);//#temp...
	
				if (!partChunk){// #this has a full data set
					String accelerometerData = chunk.substring(0,540);
					chunk = chunk.substring(540);
					timeContainer.put("accelerometer", ParseAccelerometer(accelerometerData));
				}
				String galvanicData = chunk.substring(0,100);
				timeContainer.put("galvanic", ParseGalvanic(galvanicData));
				chunk = chunk.substring(100);//#galvanic
				//sometimes a few bytes left... not sure what they are, could be part of galvanic data
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return root;
	}
	Calendar min = Calendar.getInstance();
	Calendar max = Calendar.getInstance();
	boolean isValidTime(String data){
		PasredDate result = ParseTime(data);
		return (result.date !=null &&  firstDate.get(Calendar.YEAR) == result.date.get(Calendar.YEAR) &&
				(firstDate.get(Calendar.DAY_OF_YEAR) == result.date.get(Calendar.DAY_OF_YEAR) || firstDate.get(Calendar.DAY_OF_YEAR) == result.date.get(Calendar.DAY_OF_YEAR)+1)) &&
				result.date.after(min) && result.date.before(max);
	}
	Calendar firstDate = null;
	class PasredDate{
		public String extraBytes = "";
		public Calendar date=null;
		public PasredDate(String extra, Calendar cal){
			extraBytes =  extra;
			date = cal;
		}
	}
	PasredDate ParseTime (String data){
		Calendar date = Calendar.getInstance();	
		String extra="";
		for (int j  = 0; j < data.length(); j+=2) {
			try{
			int i = data.length()-j;
			if (i<8){
				break;
			}
			String part = data.substring((i-8),i);
			long seconds = Long.parseLong(Utils.reverseBytes(part), 16);
			extra = data.substring(i);
			if (seconds < 1230768000){
				if  (firstDate == null){
					firstDate = Calendar.getInstance();
					firstDate.set(2011, 0, 1);
					firstDate.add(Calendar.SECOND, (int)seconds);
				}
				Calendar time = Calendar.getInstance();
				time.set(2011, 0, 1);
				time.add(Calendar.SECOND, (int) seconds);
				if (firstDate !=null &&  firstDate.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
						(firstDate.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR) || firstDate.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)+1)){
					date = time;
					break;
				}
					
			}
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		return new PasredDate(extra,date);
	}
	JSONArray ParseAccelerometer(String data){
		JSONArray processed = new JSONArray();
		for (int i =0; i< data.length()-3; i+=3){
			JSONArray threeAxis = new JSONArray();
			threeAxis.put(Integer.parseInt(data.substring(i, i+1), 16));
			threeAxis.put(Integer.parseInt(data.substring(i+1, i+2), 16));
			threeAxis.put(Integer.parseInt(data.substring(i+2, i+3), 16));
			processed.put(threeAxis);
		}
		return processed;
	}

	JSONArray ParseHeartRate(String data){
		JSONArray processed = new JSONArray();
		for (int i =0; i< data.length()-2; i+=2){
			processed.put(Integer.parseInt(data.substring(i, i+2), 16));	
		}
		return processed;
	}

	JSONArray ParseTemperature(String data){
		JSONArray processed = new JSONArray();
		for (int i =0; i< data.length()-2; i+=2){
			processed.put(Integer.parseInt(data.substring(i, i+2), 16));	
		}
		return processed;
	}

	JSONArray ParseGalvanic(String data){
		JSONArray processed = new JSONArray();
		processed.put(data);//I don't really know how to parse this yet, so just leave it raw
		//for (int i =0; i< data.length()-6; i+=6){
		//	processed.put(Utils.reverseBytes(data.substring(i, i+6)));
		//}
		return processed;
	}
}
