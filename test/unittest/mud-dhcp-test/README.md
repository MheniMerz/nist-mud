This tests whether the mud server fetches and installs a MUD file from the MUD profile server.

Make sure your /etc/hosts has the following entries on the CONTROLLER (OpenDaylight) host:

    203.0.113.13    www.nist.local
    203.0.113.14    www.antd.local
    127.0.0.1       toaster.nist.local

Edit the file sign-and-import.sh to point at your keystore (if not using the default location).

Generate cert and sign the mud file :
   
    sh sign-and-import.sh 

The previous step does the following: 

* Generate certificate (self signed -- for testing only! ) 
* Import into the Java kesytore (edit this to point to your keystore):
* Sign the mudfile:
* Verify the signature.
    
Start the mud profile web server on the Controller host as follows:

    sudo python mudfile-server.py

Test to see if your configuration is working (this should succeed):

    wget --no-check-certificate https://toaster.nist.local/super1/mudfile.json.sha256

Delete the previously cached rules if any :

   rm $PROJECT\_HOME/sdnmud-aggregator/karaf/target/assembly
   rm snapshot/\*
   rm journal/\*

You are now ready to run this test :

    sudo -E UNITTEST=1 python mud-test.py


