package com.maneti.basis;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.util.Log;
//maybe have aa as 'packet' start and ab as 'packet' end? 
public class Packet {

	
	public static String header = "AA";
	public static String trailer = "AB";
	String content = "";
	long lengthBytes = 0;
	Command type = Command.Unknown;
	long value = 0;
	String raw = "";
	public static int sizeCharsLength = 3;
	public Packet(String data){
		raw = new String(data);
		
		try{
			lengthBytes = Long.parseLong(data.substring(header.length(), header.length()+2),16);
			//boolean isCommand = s.matches(0[0-9A-F]0000);
			String value = data.substring(header.length()+6, header.length() + 10);
			value = value.substring(2,4)+value.substring(0,2);
			this.type = parsePacketType(value);
			content = data.substring(header.length()+10, data.length() - (trailer.length()+4));
			parseContent();
		} catch (Exception e){
			
		}
	}
	public Packet(Command packetType){
		//content = leftMap.get(packetType) + commands.get(packetType);
		type = packetType;
	}
	public Packet(Command packetType, String value){
		content = value;
		type = packetType;
		parseContent();
	}
	public static Command parsePacketType(String value){
		return getNameForValue(Integer.parseInt(value, 16));
	}
	public void parseContent(){
		if (type == Command.Response_GetPulseDataNumContainers ){
			value = Long.parseLong(reverseBytes(content.substring(2,6)), 16);
		}
		if (type == Command.Response_GetPulseDataNumBytes){
			value = Long.parseLong(reverseBytes(content.substring(2)), 16);
		}
		if ((type == Command.Command_GetPulseDataContainer || type == Command.Response_GetPulseDataContainer)){
			if(content.length() >= 6){
				value = Long.parseLong(reverseBytes(content.substring(2, 6)), 16);//container number
			} else {
				value = Long.parseLong(reverseBytes(content.substring(0, 4)), 16);//container number
			}
		}
	}
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format(Locale.CANADA ,"%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	@Override
	 public String toString(){
		if (type == Command.Response_GetPulseDataNumContainers){
			 return this.type.toString()+" - num containers: "+this.value;
		}
		if ( type == Command.Response_GetPulseDataNumBytes){
			 return this.type.toString()+" - num bytes: "+this.value+" ("+humanReadableByteCount(value, false)+")";
		}
		if ( type == Command.Command_GetPulseDataContainer){
			 return this.type.toString()+" ("+this.value+")";
		}
		if (type == Command.Response_GetPulseDataContainer){
			 return this.type.toString()+" ("+this.value+")";
		}
		return this.type.toString();
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
	  public byte[] hexStringToByteArray(String s) {
	        int len = s.length();
	        byte[] data = new byte[len / 2];
	        for (int i = 0; i < len; i += 2) {
	            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                                 + Character.digit(s.charAt(i+1), 16));
	        }
	        return data;
	    }
	public static String reverseBytes(String input){
		String result = "";
		for(int i = 0; i <= input.length()-2;i += 2){
			result+=input.substring(input.length()-(i+2), input.length()-i);
		}
		return result;
	}
	public static String paddToByte(String input){
		 String output = input;
		 if(input.length()%2 == 1){
			 output = "0"+input;
		 }
		 return output;
	}
	public static String paddToLength(String input, int length){
		 String output = input;
		 while (output.length() / 2 < length){
			 output="00"+output;
		 }
		 return output;
	}
	public static String createChecksum(String input){
		int sum = 0;
		for(int i = 0; i < input.length()-1; i += 2){
			sum+=Integer.parseInt(input.charAt(i)+""+input.charAt(i+1), 16);
		}
		String hex = paddToByte(Integer.toHexString(sum));
		if(hex.length()<4){
			hex="00"+hex;
		}
		return reverseBytes(hex);
	}
	public static String current = "";
	public static int currentTotalLength = 0;
	static boolean gotData = false;
	public static List<Packet> parseStream(String input){
		String data = current+input;
		data = data.substring(0, data.length()-2);
		int totalLength = 0;
		List<Packet> packets = new ArrayList<Packet>();
		//assuming first byte is AA... maybe bad assumption...
		try{
			while (true){
				
				totalLength = Integer.parseInt(reverseBytes(data.substring(header.length(), header.length() + sizeCharsLength*2)), 16);
				Command type = Command.Unknown;
				if(data.length()>=header.length() + sizeCharsLength*2+4){
					type = parsePacketType(reverseBytes(data.substring(header.length() + sizeCharsLength*2,header.length() + sizeCharsLength*2+4)));
				}
				int packetLength = totalLength+header.length()+trailer.length()+sizeCharsLength;
				/*if(type == Command.Response_GetPulseDataContainer){
					packetLength+=0;
				}
				if(gotData){
					Log.v("current data: ",data);
					Log.v("type : ",type.toString());
				}*/
				if(data.length() > header.length() + sizeCharsLength*2+4 && type == Command.Unknown ){
					data = data.substring(data.indexOf("abaa")+2);
					if(data.length() > header.length() + sizeCharsLength*2){
						totalLength = Integer.parseInt(reverseBytes(data.substring(header.length(), header.length() + sizeCharsLength*2)), 16);
						packetLength = totalLength+header.length()+trailer.length()+sizeCharsLength;
					}
				}
				/*if(gotData){
					Log.v("current data: ",data);
					Log.v("type : ",type.toString());
				}
				Log.v("inner length: ",totalLength+"");
				Log.v("outer length: ",packetLength+"");*/
				if((data.length()/2) >= packetLength){
					Log.v("total data: ",data);
					Log.v("packet data: ",data.substring(0,packetLength*2));
					Packet packet = new Packet(data.substring(0,packetLength*2));
					if(packet.type == Command.Response_GetPulseDataContainer){
						gotData = true;
					}
					packets.add(packet);
					data = data.substring(packetLength*2);
				}
				else {
					if(data.length()>=2){
						current = data;//.substring(0, data.length()-2);
						currentTotalLength = totalLength;
					} else{
						current = "";
					}
					
					break;
				}
			}
		} catch(Exception e){
			current = "";
		}
		
		return packets;
	}
	
	public void send(OutputStream stream){
		 String command = Integer.toHexString(getForValuetName(this.type));
		 command = reverseBytes(paddToByte(command));
		 String checksum = createChecksum(command+content);
		 String lengthByte = reverseBytes(paddToByte(Integer.toHexString((content.length()+checksum.length())/2)));
		 byte[] sendBuffer = hexStringToByteArray((header + reverseBytes(paddToLength(lengthByte, 3)) + command+content+checksum+trailer).toUpperCase(Locale.CANADA));
	     String asHex = bytArrayToHex(sendBuffer, sendBuffer.length);
         Log.v("sending packet:" +this.toString(), asHex);
        try {
        	stream.write(sendBuffer);
        } catch (IOException e) {
        	Log.v("failed to send packet:" +this.toString(), asHex);
        }
	}
	
	public enum Command{
		
	  Unknown,NullType,PresenceBroadcast,
	   Command_SetClock,Command_SmartErase,Command_ForceErase,
	  Command_SetDeviceProfile,Command_SetUserProfile,Command_SetUploaderData,
	  Command_SealUploaderData,Command_SetDataValidationMode,
	  Command_SetDataValidationADCSet,Command_SetDataStreamMode,Command_BootloaderEnter,
	  Command_ResetPerservedRAM,Command_SetActivityManualGoal,Command_SetBluetoothDisconnectTimer,
	   Command_SetBluetoothReconnectSchedule,Command_SetTimeFormat,Command_EnterUploadMode,
	   Response_SetClock,Response_SmartErase,Response_ForceErase,Response_SetDeviceProfile,
	    Response_SetUserProfile,Response_SetUploaderData,Response_SealUploaderData,
	    Response_SetDataValidationMode,Response_SetDataValidationADCSe,Response_SetDataStreamMode,
	    Response_BootloaderEnter,Response_ResetPerservedRAM,Response_SetActivityManualGoal,
	  Response_SetBluetoothDisconnectTimer,Response_SetBluetoothReconnectSchedule,Response_SetTimeFormat,
	 Response_EnterUploadMode,Command_GetClock,Command_GetPulseDataNumBytes,
	 Command_GetPulseDataNumContainers,Command_GetPulseDataContainer,
	   Command_GetFirmwareVersion,Command_GetDeviceProfile,Command_GetUserProfile,
	 Command_GetUploaderData,Command_GetEraseStatus,Command_GetBatteryLevel,
	   Command_GetBluetoothAddress,Command_GetTimeFormat,Command_GetActivityGoal,
	Command_GetBluetoothSyncSchedule,Command_GetBluetoothModuleInfo,
	 Response_GetClock,Response_GetPulseDataNumBytes,Response_GetPulseDataNumContainers,
	Response_GetPulseDataContainer,Response_GetFirmwareVersion,Response_GetDeviceProfile,
	Response_GetUserProfile,Response_GetUploaderData,Response_GetEraseStatus,
	 Response_GetBatteryLevel,Response_GetBluetoothAddress,Response_GetTimeFormat,
	  Response_GetActivityGoal,Response_GetBluetoothSyncSchedule,Response_GetBluetoothModuleInfo,
	  Unrecognized_0xFE00,Boot_Command_ExitBootloader,Boot_Command_WriteApplication,Boot_Response,
	Boot_Command_EraseApplication,OldBoot_Command_EnterBootloader,
	Response_SetDataValidationADCSet
	}

	public static Command getNameForValue(int value)
	  {
	    switch (value)
	    {
	    default:
	      return Command.Unknown;
	    case 0:
	      return Command.NullType;
	    case 768:
	      return Command.PresenceBroadcast;
	    case 1024:
	      return Command.Command_SetClock;
	    case 1025:
	      return Command.Command_SmartErase;
	    case 1026:
	      return Command.Command_ForceErase;
	    case 1027:
	      return Command.Command_SetDeviceProfile;
	    case 1028:
	      return Command.Command_SetUserProfile;
	    case 1029:
	      return Command.Command_SetUploaderData;
	    case 1030:
	      return Command.Command_SealUploaderData;
	    case 1031:
	      return Command.Command_SetDataValidationMode;
	    case 1032:
	      return Command.Command_SetDataValidationADCSet;
	    case 1033:
	      return Command.Command_SetDataStreamMode;
	    case 1034:
	      return Command.Command_BootloaderEnter;
	    case 1035:
	      return Command.Command_ResetPerservedRAM;
	    case 1036:
	      return Command.Command_SetActivityManualGoal;
	    case 1037:
	      return Command.Command_SetBluetoothDisconnectTimer;
	    case 1038:
	      return Command.Command_SetBluetoothReconnectSchedule;
	    case 1039:
	      return Command.Command_SetTimeFormat;
	    case 1040:
	      return Command.Command_EnterUploadMode;
	    case 1280:
	      return Command.Response_SetClock;
	    case 1281:
	      return Command.Response_SmartErase;
	    case 1282:
	      return Command.Response_ForceErase;
	    case 1283:
	      return Command.Response_SetDeviceProfile;
	    case 1284:
	      return Command.Response_SetUserProfile;
	    case 1285:
	      return Command.Response_SetUploaderData;
	    case 1286:
	      return Command.Response_SealUploaderData;
	    case 1287:
	      return Command.Response_SetDataValidationMode;
	    case 1288:
	      return Command.Response_SetDataValidationADCSet;
	    case 1289:
	      return Command.Response_SetDataStreamMode;
	    case 1290:
	      return Command.Response_BootloaderEnter;
	    case 1291:
	      return Command.Response_ResetPerservedRAM;
	    case 1292:
	      return Command.Response_SetActivityManualGoal;
	    case 1293:
	      return Command.Response_SetBluetoothDisconnectTimer;
	    case 1294:
	      return Command.Response_SetBluetoothReconnectSchedule;
	    case 1295:
	      return Command.Response_SetTimeFormat;
	    case 1296:
	      return Command.Response_EnterUploadMode;
	    case 1536:
	      return Command.Command_GetClock;
	    case 1537:
	      return Command.Command_GetPulseDataNumBytes;
	    case 1538:
	      return Command.Command_GetPulseDataNumContainers;
	    case 1539:
	      return Command.Command_GetPulseDataContainer;
	    case 1540:
	      return Command.Command_GetFirmwareVersion;
	    case 1541:
	      return Command.Command_GetDeviceProfile;
	    case 1542:
	      return Command.Command_GetUserProfile;
	    case 1543:
	      return Command.Command_GetUploaderData;
	    case 1544:
	      return Command.Command_GetEraseStatus;
	    case 1545:
	      return Command.Command_GetBatteryLevel;
	    case 1546:
	      return Command.Command_GetBluetoothAddress;
	    case 1547:
	      return Command.Command_GetTimeFormat;
	    case 1548:
	      return Command.Command_GetActivityGoal;
	    case 1549:
	      return Command.Command_GetBluetoothSyncSchedule;
	    case 1550:
	      return Command.Command_GetBluetoothModuleInfo;
	    case 1792:
	      return Command.Response_GetClock;
	    case 1793:
	      return Command.Response_GetPulseDataNumBytes;
	    case 1794:
	      return Command.Response_GetPulseDataNumContainers;
	    case 1795:
	      return Command.Response_GetPulseDataContainer;
	    case 1796:
	      return Command.Response_GetFirmwareVersion;
	    case 1797:
	      return Command.Response_GetDeviceProfile;
	    case 1798:
	      return Command.Response_GetUserProfile;
	    case 1799:
	      return Command.Response_GetUploaderData;
	    case 1800:
	      return Command.Response_GetEraseStatus;
	    case 1801:
	      return Command.Response_GetBatteryLevel;
	    case 1802:
	      return Command.Response_GetBluetoothAddress;
	    case 1803:
	      return Command.Response_GetTimeFormat;
	    case 1804:
	      return Command.Response_GetActivityGoal;
	    case 1805:
	      return Command.Response_GetBluetoothSyncSchedule;
	    case 1806:
	      return Command.Response_GetBluetoothModuleInfo;
	    case 65024:
	      return Command.Unrecognized_0xFE00;
	    case 65530:
	      return Command.Boot_Command_ExitBootloader;
	    case 65531:
	      return Command.Boot_Command_WriteApplication;
	    case 65532:
	      return Command.Boot_Response;
	    case 65533:
	      return Command.Boot_Command_EraseApplication;
	    case 65534:
	    }
	    return Command.OldBoot_Command_EnterBootloader;
	  }
	public static int getForValuetName(Command type)
	  {
	    switch (type)
	    {
	    default:
	      return 0;
	    case NullType:
	      return 0;
	    case PresenceBroadcast:
	      return 768;
	    case Command_SetClock:
	      return 1024;
	    case Command_SmartErase:
	      return 1025;
	    case Command_ForceErase:
	      return 1026;
	    case Command_SetDeviceProfile:
	      return 1027;
	    case Command_SetUserProfile:
	      return 1028;
	    case Command_SetUploaderData:
	      return 1029;
	    case Command_SealUploaderData:
	      return 1030;
	    case Command_SetDataValidationMode:
	      return 1031;
	    case Command_SetDataValidationADCSet:
	      return 1032;
	    case Command_SetDataStreamMode:
	      return 1033;
	    case Command_BootloaderEnter:
	      return 1034;
	    case Command_ResetPerservedRAM:
	      return 1035;
	    case Command_SetActivityManualGoal:
	      return 1036;
	    case Command_SetBluetoothDisconnectTimer:
	      return 1037;
	    case Command_SetBluetoothReconnectSchedule:
	      return 1038;
	    case Command_SetTimeFormat:
	      return 1039;
	    case Command_EnterUploadMode:
	      return 1040;
	    case Response_SetClock:
	      return 1280;
	    case Response_SmartErase:
	      return 1281;
	    case Response_ForceErase:
	      return 1282;
	    case Response_SetDeviceProfile:
	      return 1283;
	    case Response_SetUserProfile:
	      return 1284;
	    case Response_SetUploaderData:
	      return 1285;
	    case Response_SealUploaderData:
	      return 1286;
	    case Response_SetDataValidationMode:
	      return 1287;
	    case Response_SetDataValidationADCSet:
	      return 1288;
	    case Response_SetDataStreamMode:
	      return 1289;
	    case Response_BootloaderEnter:
	      return 1290;
	    case Response_ResetPerservedRAM:
	      return 1291;
	    case Response_SetActivityManualGoal:
	      return 1292;
	    case Response_SetBluetoothDisconnectTimer:
	      return 1293;
	    case Response_SetBluetoothReconnectSchedule:
	      return 1294;
	    case Response_SetTimeFormat:
	      return 1295;
	    case Response_EnterUploadMode:
	      return 1296;
	    case Command_GetClock:
	      return 1536;
	    case Command_GetPulseDataNumBytes:
	      return 1537;
	    case Command_GetPulseDataNumContainers:
	      return 1538;
	    case Command_GetPulseDataContainer:
	      return 1539;
	    case Command_GetFirmwareVersion:
	      return 1540;
	    case Command_GetDeviceProfile:
	      return 1541;
	    case Command_GetUserProfile:
	      return 1542;
	    case Command_GetUploaderData:
	      return 1543;
	    case Command_GetEraseStatus:
	      return 1544;
	    case Command_GetBatteryLevel:
	      return 1545;
	    case Command_GetBluetoothAddress:
	      return 1546;
	    case Command_GetTimeFormat:
	      return 1547;
	    case Command_GetActivityGoal:
	      return 1548;
	    case Command_GetBluetoothSyncSchedule:
	      return 1549;
	    case Command_GetBluetoothModuleInfo:
	      return 1550;
	    case Response_GetClock:
	      return 1792;
	    case Response_GetPulseDataNumBytes:
	      return 1793;
	    case Response_GetPulseDataNumContainers:
	      return 1794;
	    case Response_GetPulseDataContainer:
	      return 1795;
	    case Response_GetFirmwareVersion:
	      return 1796;
	    case Response_GetDeviceProfile:
	      return 1797;
	    case Response_GetUserProfile:
	      return 1798;
	    case Response_GetUploaderData:
	      return 1799;
	    case Response_GetEraseStatus:
	      return 1800;
	    case Response_GetBatteryLevel:
	      return 1801;
	    case Response_GetBluetoothAddress:
	      return 1802;
	    case Response_GetTimeFormat:
	      return 1803;
	    case Response_GetActivityGoal:
	      return 1804;
	    case Response_GetBluetoothSyncSchedule:
	      return 1805;
	    case Response_GetBluetoothModuleInfo:
	      return 1806;
	    case Unrecognized_0xFE00:
	      return 65024;
	    case Boot_Command_ExitBootloader:
	      return 65530;
	    case Boot_Command_WriteApplication:
	      return 65531;
	    case Boot_Response:
	      return 65532;
	    case Boot_Command_EraseApplication:
	      return 65533;
	    case OldBoot_Command_EnterBootloader:
	    }
	    return 65534;
	  }
	
}
