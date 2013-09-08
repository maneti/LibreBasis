package com.maneti.basis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.ScrollView;
import android.widget.TextView;

public class BasisActivity extends Activity implements Runnable {
	public interface PacketHandler{
		public Packet.Command type = Packet.Command.Unknown;
		public void Handle(Packet packet);
	}
	public static List<PacketHandler> handlers = new ArrayList<PacketHandler>();
	
	final Handler timeoutHandler = new Handler();
    final Runnable cancelTask = new Runnable(){
    	public void run(){
    		networkThread.cancel();
    	}
    };
	private ConnectedThread networkThread = null;
	private final static int REQUEST_ENABLE_BT = 1;
	BluetoothAdapter mBluetoothAdapter;
	ArrayList<Packet> packets = new ArrayList<Packet>();
	final Handler listenHandler = new Handler();
	private TextView myText = null;
	public ScrollView scrollView = null;
	public static BasisActivity instance;
	static String log = "Starting app:";
	public Handler updateHandler = new Handler();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;

		setContentView(R.layout.activity_basis);
		File folder = new File(Environment.getExternalStorageDirectory() + "/basis");
		if (!folder.exists()) {
		    folder.mkdir();
		}
		
		handlers.add(
				new PacketHandler(){
					public Packet.Command type = Packet.Command.Response_GetPulseDataContainer;
					public void Handle(Packet packet) {
						if(packet.type == this.type){
							Calendar cal = Calendar.getInstance();
							
							File file = new File(Environment.getExternalStorageDirectory() + "/basis/basis_raw_log_chunk_num_"+packet.value+"_"+(cal.getTime().toString()).replace("/", ".")+".log");
							if (!file.exists()) {
						        try {
						            file.createNewFile();
						            FileWriter filewriter = new FileWriter(file,false);
						            filewriter.write(packet.content);
						            filewriter.flush();
						            filewriter.close();
						            
						        } catch (IOException e) {
						            e.printStackTrace();
						        }
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
								networkThread.toSendSequence.add(new Packet(Packet.Command.Command_GetPulseDataContainer, "00"+Packet.reverseBytes(Packet.paddToLength(Packet.paddToByte(Integer.toHexString(i)), 2 ))));
								networkThread.toReceiveSequence.add(new Packet(Packet.Command.Response_GetPulseDataContainer));
								networkThread.toReceiveSequence.add(new Packet(Packet.Command.PresenceBroadcast));
							}
						}
					}
				}
			);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			log+=System.getProperty("line.separator")+"No Bluetooth? this app will not work! ";
		}
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		//Context context = getApplicationContext();
		//int duration = Toast.LENGTH_SHORT;
		CharSequence text = "";
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		    	if(device.getName().equalsIgnoreCase("Basis B1")){
			    	text = device.getName() + " " + device.getAddress();
			    	//Toast toast = Toast.makeText(context, text, duration);
			    	//toast.show();
			    	log+=System.getProperty("line.separator")+"Found paired watch: "+text;
			    	AcceptThread at = new AcceptThread();
			    	at.start();
		    	}
		    }
		}
		myText = new TextView(this);
		scrollView = new ScrollView(this);
		scrollView.addView(myText);
		myText.setText(log);
		setContentView(scrollView);
		
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
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("basis", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
	        } catch (IOException e) { }
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
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;

	    public void updateLog(String newLine){
	    	log+=System.getProperty("line.separator")+ newLine;
	    	instance.updateHandler.post(instance);
	    }
	    public ConnectedThread(BluetoothSocket socket) {
	    	networkThread = this;
	    	
	    	toSendSequence.add(new Packet(Packet.Command.Command_GetPulseDataNumContainers));
	    	toSendSequence.add(new Packet(Packet.Command.Command_GetPulseDataNumBytes));
	    	toSendSequence.add(new Packet(Packet.Command.Command_SetBluetoothDisconnectTimer, Packet.reverseBytes(Packet.paddToLength(Packet.paddToByte(Integer.toHexString(90)), 2 ))));
	    	toSendSequence.add(new Packet(Packet.Command.Command_EnterUploadMode));

	    	toReceiveSequence.add(new Packet(Packet.Command.PresenceBroadcast));
	    	toReceiveSequence.add(new Packet(Packet.Command.Response_GetPulseDataNumContainers));
	    	toReceiveSequence.add(new Packet(Packet.Command.Response_GetPulseDataNumBytes));
	    	toReceiveSequence.add(new Packet(Packet.Command.Response_SetBluetoothDisconnectTimer));
	    	toReceiveSequence.add(new Packet(Packet.Command.Response_EnterUploadMode));
	    	
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

	    List<Packet> toSendSequence = new ArrayList<Packet>();
	    List<Packet> toReceiveSequence = new ArrayList<Packet>();
	    public Packet processNextPacket(Packet packet){
	    	if(nextResponsePacketIndex < toReceiveSequence.size() && toReceiveSequence.get(nextResponsePacketIndex).type == packet.type){
	    		nextResponsePacketIndex+=1;
	    		if(nextResponsePacketIndex <= toSendSequence.size()){
	    			return toSendSequence.get(nextResponsePacketIndex-1);
	    		}
	    	}
	    	return null;
	    }
	    int nextResponsePacketIndex = 0;
	    public void processNewPackets(List<Packet> newPackets){
	    	if(newPackets.size() > 0){
            	for(Packet packet :newPackets){
                	packets.add(packet);
                	updateLog("Got packet: "+ packet.toString());
                	Log.v("got packet: ", packet.toString());
                	for(PacketHandler handler : handlers){
                		handler.Handle(packet);
                	}
            		Packet toSend = processNextPacket(packet);
            		if(toSend != null){
            			toSend.send(mmOutStream);
            			updateLog("Sending packet: "+toSend.toString());
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
	               // totalSinceLastPacket+=bytes;
	               // Log.v("current part:",Packet.current);
	                
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
