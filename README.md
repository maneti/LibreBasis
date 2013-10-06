LibreBasis
==========

Open source android app for with the mybasis watch.

Features:
  Pair with watch (not needed if you already paired it using the offical app)
  Download data from watch
  Delete data on watch

you'll need to open this app (you'll need to build the apk yourself, later once its less alpha I'll include a prebuilt apk(there is an apk in the bin folder, but it's my current debug build, so no guarantee it works))

then press the button on the watch (or wait a few minutes for the watch to try to auto sync) and the app should
show the commands sent and received. it will find out how many 'containers' (64k chunks of data), then how many bytes of
data are available. it will then try to download all the data and store it in files (one file pre 64k chunck) in sdcard/basis/

It can store the data as raw hex or as parsed JSON, the parsing is correct as far as I know, but I can't guarantee it will always correct.
I currently only parse heart rate, temperature and accelerometer. I've left the moisture data raw in the output so it isn't lost, I hope to parse that too soon, but you can try now... and save me the work of doing it ;) 
There are a few other small bits of data that I don't parse (because I have no idea what they are yet)
Heart rate and temperature simple, because they are just numbers, accelerometer is much harder because it's raw accelerometer force data, so it might be hard to figure out steps from it... another thing to to...

See the JSON format spec file for more details about the output format.
I intend allow outputing as csv soon too.

There are a number of options in the settings menu, only some of these have been implemented yet, but I plan to implement the others over the next couple of weeks

known issues:
1: if the socket bind fails the app will FC (for now manualy turning off and on bluetooth normally fixes this, in the future
we should to this in code)

2: if the offical app is running, this app my not get the socket correctly, simply stopping the offical app is not enough,
because the socket is still bound to it, the only why around this is to restart the phone (and make sure the offical app is
not set to start at boot)



I'll polish the app up a bit, but I'm not planning to add any fancy graphs or ablity to sync with your favorite life tracking
site. I am happy to accept pull requests, so if you want to add stuff like that, go for it. also, I only have an android device
so you might want to port this to iOS or desktop OSes.

note: I've only used this on a Nexus 4, so although I think it should be fine on other devices, I'm not sure.
Also, I've only tried this with firmware version v2.43 – 2.47, so it may not work with other versions
