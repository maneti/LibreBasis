package com.maneti.basis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

public class BlueToothInterface {
	public interface PacketHandler{
		public Packet.Command type = Packet.Command.Unknown;
		public void Handle(Packet packet);
		
	}
	public static List<PacketHandler> handlers = new ArrayList<PacketHandler>();
	private final static int REQUEST_ENABLE_BT = 1;
	final Handler listenHandler = new Handler();
	BluetoothAdapter mBluetoothAdapter;
	BasisActivity mainActivity;
	ArrayList<Packet> packets = new ArrayList<Packet>();
	public static String deviceName = "Basis B1";
	UUID uid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	ArrayList<String> tempFiles = new ArrayList<String>();
	int numberOfBlocks = -1;
	static List<Packet> toSendSequence = new ArrayList<Packet>();
	static List<Packet> toReceiveSequence = new ArrayList<Packet>();
	final Handler timeoutHandler = new Handler();
    final Runnable cancelTask = new Runnable(){
    	public void run(){
    		networkThread.cancel();
    	}
    };
    private ConnectedThread networkThread = null;
    
    public BlueToothInterface(){
    	mBluetoothAdapter =  BluetoothAdapter.getDefaultAdapter();
    	mainActivity = BasisActivity.instance;
    	if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
    		mainActivity.Log("No Bluetooth? this app will not work!");
		}
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    mainActivity.startActivityForResult(enableBtIntent, BlueToothInterface.REQUEST_ENABLE_BT);
		}
    	handlers.add(
			new PacketHandler(){
				public Packet.Command type = Packet.Command.Response_GetPulseDataContainer;
				public void Handle(Packet packet) {
					if(packet.type == this.type){
						 Calendar cal = Calendar.getInstance();
						 String fileName = Environment.getExternalStorageDirectory() + "/basis/basis_raw_log_chunk_num_"+packet.value+"_"+(cal.getTime().toString()).replace("/", ".")+".log";
			             File file = new File(fileName);
						 if (!file.exists()) {
						 	try {
						    	file.createNewFile();
						        FileWriter filewriter = new FileWriter(file,false);
					            filewriter.write(packet.content);
					            filewriter.flush();
					            filewriter.close();
					            tempFiles.add(fileName);
						    } catch (IOException e) {
						    	e.printStackTrace();
						    }
						}
						if(numberOfBlocks == tempFiles.size()){
							new ProcessThread().run();
						}
					}
				}
			}
		);
		handlers.add(
			new PacketHandler(){
				public Packet.Command type = Packet.Command.Response_GetPulseDataNumContainers;
				public void Handle(Packet packet) {
					if(packet.type == this.type){
						for(int i = 0; i < packet.value; i ++){
							toSendSequence.add(new Packet(Packet.Command.Command_GetPulseDataContainer, Utils.reverseBytes(Utils.paddToLength(Utils.paddToByte(Integer.toHexString(i)), 2 ))));
							toReceiveSequence.add(new Packet(Packet.Command.Response_GetPulseDataContainer));
							
						}
						if(mainActivity.prefs.getBoolean(mainActivity.getResources().getString(R.string.pref_delete), true)){
							toSendSequence.add(new Packet(Packet.Command.Command_SmartErase));
							toReceiveSequence.add(new Packet(Packet.Command.Response_SmartErase));
						}
						numberOfBlocks = (int)packet.value;
						//networkThread.toSendSequence.add(new Packet(Packet.Command.Command_SmartErase));
					}
				}
			}
		);
		
		
		//Context context = getApplicationContext();
		//int duration = Toast.LENGTH_SHORT;
		CharSequence text = "";
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		    	if(device.getName().equalsIgnoreCase(deviceName)){
			    	text = device.getName() + " " + device.getAddress();
			    	//Toast toast = Toast.makeText(context, text, duration);
			    	//toast.show();
			    	mainActivity.Log("Found paired watch: "+text);
			    	Listen();
		    	}
		    }
		} else{
			mainActivity.Log("No paired watch found.");
		}
    }
    public void Listen(){
    	AcceptThread at = new AcceptThread();
    	at.start();
    }
    public void Pair(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mainActivity.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mainActivity.registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
        mainActivity.Log("Searching for watch. Remember to put it in pairing mode");
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED && device.getName().equalsIgnoreCase(deviceName)) {
                	String text = device.getName() + " " + device.getAddress();
			    	mainActivity.Log("Found unpaired watch: "+text);
			    	try {
			            Log.d("pairDevice()", "Start Pairing...");
			            Method m = device.getClass().getMethod("createBond", (Class[]) null);
			            m.invoke(device, (Object[]) null);
			            Log.d("pairDevice()", "Pairing finished.");
			        } catch (Exception e) {
			            Log.e("pairDevice()", e.getMessage());
			        }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            	mainActivity.Log("Done searching for watches.");
            }
        }
    };
    public void SetTime(int year, int month, int day, int hour, int minute){
    	//todo implement setting time
    	Calendar time = Calendar.getInstance();
    	time.set(year, month, day, hour, minute);
    	toReceiveSequence.add(new Packet(Packet.Command.PresenceBroadcast));
    	toSendSequence.add(new Packet(Packet.Command.Command_SetClock, time));

    }
    public void ForceDelete(){
		toSendSequence.add(new Packet(Packet.Command.Command_SmartErase));

    	toReceiveSequence.add(new Packet(Packet.Command.Response_SmartErase));
    	Listen();
    }
    private class ProcessThread extends Thread {
	    public ProcessThread() {
	        
	    }
	 
	    public void run() {
	    	Decoder decoder = new Decoder();
	    	for(String fileName : tempFiles){
	    		File file = new File(fileName);
	    		
	    		StringBuilder sb = new StringBuilder();
    	        String line = "";
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					line = br.readLine();
					while (line != null) {
		    	       	 sb.append(line);
		    	       	 try {
							line = br.readLine();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    	        }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

    	        
    	        decoder.decode(sb.toString());
    	        if(!mainActivity.prefs.getBoolean(mainActivity.getResources().getString(R.string.pref_raw), false)){
    	        	file.delete();
    	        }
	    	}
	    	Iterator<String> iter = decoder.root.keys();
	    	List<String> minutes = new ArrayList<String>();
    	    while (iter.hasNext()) {
    	        String key = iter.next();
    	        minutes.add(key);
    	        //Object value = decoder.root.get(key);
    	    }
    	    Collections.sort(minutes);
    	    String fileName = Environment.getExternalStorageDirectory() + "/basis/basis_log_"+minutes.get(0)+"_to_"+minutes.get(minutes.size()-1)+".json";
    	    
    	   
			if(mainActivity.prefs.getBoolean(mainActivity.getResources().getString(R.string.pref_json), true)){
				File file = new File(fileName+".json");
	    		if (!file.exists()) {
	    	        try {
	    	            file.createNewFile();
	    	            FileWriter filewriter = new FileWriter(file,false);
	    	            filewriter.write(decoder.root.toString());
	    	            filewriter.flush();
	    	            filewriter.close();
	    	            
	    	        } catch (IOException e) {
	    	            e.printStackTrace();
	    	        }
	    		}
			}
			if(mainActivity.prefs.getBoolean(mainActivity.getResources().getString(R.string.pref_csv), true)){
				File file = new File(fileName+".csv");
	    		if (!file.exists()) {
	    	        try {
	    	            file.createNewFile();
	    	            FileWriter filewriter = new FileWriter(file,false);
	    	            for(String key : minutes){
	    	            	
	    	            	 try {
	    	            		Date time = Decoder.isoFormat.parse(key);
								JSONObject minute = (JSONObject) decoder.root.get(key);
								JSONArray heartRate = (JSONArray) minute.get("heart rate");
								JSONArray accelerometer =  minute.get("accelerometer") == null ? null : (JSONArray) minute.get("accelerometer");
								JSONArray temperature =  minute.get("temperature") == null ? null : (JSONArray) minute.get("temperature");
								for(int i = 0; i < 60; i +=1){
		    	            		time.setSeconds(i);
		    	            		String line = "";
		    	            		line+=Decoder.isoFormat.format(time)+",";
		    	            		line+=heartRate.get(i)+",";
		    	            		if(accelerometer!=null){
		    	            			line+=accelerometer.get(i*3).toString().replace(",", "-")+","+accelerometer.get(i*3+1).toString().replace(",", "-")+","+accelerometer.get(i*3+2).toString().replace(",", "-")+",";
		    	            		} else{
		    	            			line+=",,,";
		    	            		}
		    	            		if(i%7==0 || i==59){
		    	            			if(i%7==0){
		    	            				line+=heartRate.get(i/7)+",";
			    	            		} else{
			    	            			line+=heartRate.get(7)+",";
			    	            		}
		    	            		} else{
		    	            			line+=",";
		    	            		}
		    	            		filewriter.write(line+"\n");
		    	            	}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	    	            	
	    	            }
	    	           
	    	            filewriter.flush();
	    	            filewriter.close();
	    	            
	    	        } catch (IOException e) {
	    	            e.printStackTrace();
	    	        }
	    		}
			}
	    }
	}
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("basis", uid);
	        } catch (IOException e) { 
	        	int i =0;
	        }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
	                break;//TODO: try turning off and on bluetooth
	            }
	            // If a connection was accepted
	            if (socket != null) {
	                // Do work to manage the connection (in a separate thread)
	              //  manageConnectedSocket(socket);
	            	ConnectedThread ct = new ConnectedThread(socket);
	            	//listenHandler.postDelayed(ct, 100);
	            	ct.start();
	                try {
						mmServerSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	                break;
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	public void Download(){
		toSendSequence.add(new Packet(Packet.Command.Command_GetPulseDataNumContainers));
    	toSendSequence.add(new Packet(Packet.Command.Command_GetPulseDataNumBytes));
    	toSendSequence.add(new Packet(Packet.Command.Command_SetBluetoothDisconnectTimer, Utils.reverseBytes(Utils.paddToLength(Utils.paddToByte(Integer.toHexString(90)), 2 ))));
    	toSendSequence.add(new Packet(Packet.Command.Command_EnterUploadMode));

    	toReceiveSequence.add(new Packet(Packet.Command.PresenceBroadcast));
    	toReceiveSequence.add(new Packet(Packet.Command.Response_GetPulseDataNumContainers));
    	toReceiveSequence.add(new Packet(Packet.Command.Response_GetPulseDataNumBytes));
    	toReceiveSequence.add(new Packet(Packet.Command.Response_SetBluetoothDisconnectTimer));
    	toReceiveSequence.add(new Packet(Packet.Command.Response_EnterUploadMode));
	}
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;

	  
	    public ConnectedThread(BluetoothSocket socket) {
	    	networkThread = this;
	    	if(mainActivity.prefs.getBoolean(mainActivity.getResources().getString(R.string.pref_download), true)){
	    		Download();
			}
	    	
	    	
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	    String bytArrayToHex(byte[] a, int length) {
	    	   StringBuilder sb = new StringBuilder();
	    	   int index = 0;
	    	   for(byte b: a){
	    		   if(index>length){
	    			   break;
	    		   }
	    		   index+=1;
	    		   sb.append(String.format("%02x", b&0xff));
	    	   }
	    	   return sb.toString();
	    	}

	   
	    public Packet processNextPacket(Packet packet){
	    	if(toReceiveSequence.size() > 0 && toReceiveSequence.get(0).type == packet.type){
	    		Packet current = toReceiveSequence.get(0);
	    		toReceiveSequence.remove(current);
	    		if(toSendSequence.size() > 0){
	    			Packet next = toSendSequence.get(0);
	    			toSendSequence.remove(next);
	    			return next;
	    		}
	    	}
	    	return null;
	    }
	    public void processNewPackets(List<Packet> newPackets){
	    	if(newPackets.size() > 0){
            	for(Packet packet :newPackets){
                	packets.add(packet);
                	mainActivity.Log("Got packet: "+ packet.toString());
                	Log.v("got packet: ", packet.toString());
                	for(PacketHandler handler : handlers){
                		handler.Handle(packet);
                	}
            		Packet toSend = processNextPacket(packet);
            		if(toSend != null){
            			toSend.send(mmOutStream);
            			mainActivity.Log("Sending packet: "+toSend.toString());
            		}
            	}
            }
	    }
	    public void run() {
	        byte[] buffer = new byte[10024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        // Keep listening to the InputStream until an exception occurs
	        int totalSinceLastPacket = 0;
		    
	        while (true) {
	            try {
	                // Read from the InputStream
	            	timeoutHandler.removeCallbacks(cancelTask);
	            	timeoutHandler.postDelayed(cancelTask, 120000);
	            	int maxBytes = buffer.length;
	            	if(Packet.current.length() > 0){
	            		maxBytes = Math.min(buffer.length, Packet.currentTotalLength);
	            	}
	                bytes = mmInStream.read(buffer, 0, maxBytes);
	                String s =  bytArrayToHex(buffer, bytes);
	                List<Packet> got = Packet.parseStream(s);
	                if(got.size() > 0){
	                	processNewPackets(got);
	                	totalSinceLastPacket=0;
	                }
	                
	                Log.v("totalSinceLastPacket:",totalSinceLastPacket+"");
	                Log.v("expected:",Packet.currentTotalLength+"");
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
}
