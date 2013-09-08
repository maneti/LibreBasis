LibreBasis
==========

Open source android app for interfacing with the mybasis watch.

you'll need to have already paired your watch with the android device (possibly with the offical app) and open 
this app (you'll need to build the apk yourself, later once its less alpha I'll include a prebuilt apk)

then press the button on the watch (or wait a few minutes for the watch to try to auto sync) and the app should
show the commands sent and received. it will find out how many 'containers' (64k chunks of data), then how many bytes of
data are available. it will then try to download all the data and store it in files (one file pre 64k chunck) in sdcard/basis/


known issues:
1: if the socket bind fails the app will FC (for now manualy turning off and on bluetooth normally fixes this, in the future
we should to this in code)

2: if the offical app is running, this app my not get the socket correctly, simply stopping the offical app is not enough,
because the socket is still bound to it, the only why around this is to restart the phone (and make sure the offical app is
not set to start at boot)



the data is raw, so its not much use (but might be useful if you're trying to decode it yourself), so my goal atm is to dacode it
later I'll polish the app up a bit, but I'm not planning to add any fancy graphs or ablity to sync with your favorite life tracking
site. but I am happy to accept pull requests, so if you want to add stuff like that, go for it. also, I only have and android device
so you might want to port this to iOS or desktop OSes.

note: I've only used this on a Nexus 4, so although I think it should be fine on other devices, I'm not sure
also, I've only tried this with firmware version v2.43 – 2.47, so it may not work with other versions
