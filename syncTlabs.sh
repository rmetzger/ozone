echo "Syncing local 'lib' dir, with remote one"

rsync   --progress -avze ssh stratosphere-dist/target/stratosphere-dist-0.2-ozone-bin/stratosphere-0.2-ozone/lib/ hadoop@192.168.82.148:/home/hadoop/robert/stratosphere-0.2-ozone/lib/
rsync   --progress -avze ssh stratosphere-dist/target/stratosphere-dist-0.2-ozone-bin/stratosphere-0.2-ozone/lib_clients/ hadoop@192.168.82.148:/home/hadoop/robert/stratosphere-0.2-ozone/lib_clients/