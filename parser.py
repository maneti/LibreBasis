import datetime
import json
startDate = datetime.datetime(2011,1,1,0,0,0)
def reverse(s):
	return "".join(reversed([s[i:i+2] for i in range(0, len(s), 2)]))

#saved = open("basis_raw_log_chunk_num_0_Sat+Sep+28+18-56-30+NZST+2013.log", "r")
#saved = open("basis_raw_log_chunk_num_1_Sat+Sep+28+18-56-35+NZST+2013.log", "r")
saved = open("test.log", "r")
data = saved.read()
content = data[6:]
chunkNumberString = data[2:6]
output = {}
output['block'] = str(int(reverse(chunkNumberString), 16))

firstDate = None
def parseTime(string):
	global firstDate
	#areas = [string[-10:], string[-12:-2], string[-14:-4], string[0:10], string[2:12] ]
	date = None

	extra=''
	#print range(0,len(string),2)
	for j in range(0,len(string),2):
		i = len(string)-j
		#print i
		if i<8:
			break
		part = string[(i-8):i]
		seconds = int(reverse(part), 16)
		extra = string[i:]

		#print string,part
		if seconds < 1230768000:
			if  firstDate is None:
				firstDate = startDate + datetime.timedelta(0,seconds)
			time = startDate + datetime.timedelta(0,seconds)
			#print string, extra, seconds,time,string[-(i-8):i]
			if firstDate is not None and  firstDate.year == time.year and  firstDate.month == time.month and  (firstDate.day == time.day or firstDate.day+1 == time.day) and time > datetime.datetime(2011,1,1,0,0,0) and time <  datetime.datetime(2050,1,1,0,0,0):
				date = time
				break
	
	return (extra,date)
def isValidTime(string):
	extra, date = parseTime(string)
	return date is not None and  firstDate.year == date.year and  firstDate.month == date.month and  (firstDate.day == date.day or firstDate.day+1 == date.day) and date > datetime.datetime(2011,1,1,0,0,0) and date <  datetime.datetime(2050,1,1,0,0,0)
def parseHeartRate(string):
	processed = [];
	for i in range(0, len(string)-2, 2):
		processed.append(int(reverse(string[i:i+2]), 16))
	return processed
def parseGalvanic(string):
	return string#I don't really know how to parse this yet, so just leave it raw
	processed = [];
	for i in range(0, len(string)-6, 6):
		processed.append(reverse(string[i:i+6]))
	return processed	
def parseTemperature(string):
	processed = [];
	for i in range(0, len(string)-2, 2):
		processed.append(int(reverse(string[i:i+2]), 16))	
	return processed

def parseAccelerometer(string):
	processed = [];
	for i in range(0, len(string)-3, 3):
		processed.append([int(reverse(string[i]), 16),int(reverse(string[i+1]), 16),int(reverse(string[i+2]), 16)])
	return processed

while '2900' in content:
	
	chunk = content[0:content.index('2900')+4]
	content = content[content.index('2900')+4:]
	timeLength = 32
	if  firstDate is None and content.index('2900')>0:
		parseTime(content[0:8]) 
	while not isValidTime(content[0:timeLength]):
		chunk = chunk+content[0:content.index('2900')+4]
		content = content[content.index('2900')+4:]

	extra, time = parseTime(chunk[0:timeLength])
	
	if time is None:
		#print 'bad time: '+chunk[0:timeLength]
		#print chunk
		continue#currupt(?) block without time, skip
		#break
	chunk = chunk[:-4]
	if len(chunk) < 80:
		#print 'skipping:'+chunk
		continue
	extra, time = parseTime(chunk[0:timeLength])
	
	chunk=extra+chunk[timeLength:]#time
	timeContainer = {}
		
	if str(time) in output:
		timeContainer = output[str(time)]
	output[str(time)] = timeContainer
	partChunk = len(chunk) < 400
	
	heartData = chunk[-120:]
	if not isValidTime(chunk[-128:-120]):
		heartData = chunk[-122:-2]
		chunk=chunk[:-2]
	timeContainer['heart rate'] = parseHeartRate(heartData)
	chunk = chunk[:-120]#heart rate
	chunk = chunk[:chunk.rindex('2b00')]#another datetime, not sure why
	if partChunk:#this chunk only has heart rate and galvanic data
		if len(chunk) > 150:
			chunk = chunk[:chunk.rindex('3c00')]#also sometimes extra unknown minichunk
			chunk = chunk[:-12]#another datetime, not sure why
	else: #this has a full data set
		chunk = chunk[:-12]#unknown
	tempData = chunk[-16:]
	timeContainer['temperature'] = parseHeartRate(tempData)
	chunk = chunk[:-16]#temp...

	if not partChunk: #this has a full data set
		accelerometerData = chunk[0:540];
		chunk = chunk[540:]
		timeContainer['accelerometer'] = parseAccelerometer(accelerometerData)
	galvanicData = chunk[:100]
	timeContainer['galvanic_raw'] = parseGalvanic(galvanicData)
	chunk = chunk[100:]#galvanic

print json.dumps(output,separators=(',',':'))#, indent=4, sort_keys=True
