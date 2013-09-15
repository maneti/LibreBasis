package com.maneti.basis;

public class Utils {
	 public static String bytArrayToHex(byte[] a, int length) {
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
	 public static byte[] hexStringToByteArray(String s) {
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
}
