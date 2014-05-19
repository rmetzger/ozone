---
layout: inner_docs_v04
title:  "Stratosphere On YARN"
sublinks:
  - {anchor: "session", title: "Start Stratosphere Session"}
  - {anchor: "submission", title: "Job Submission"}
  - {anchor: "build", title: "Advanced Building"}
  - {anchor: "background", title: "Background"}
---

## Stratosphere on YARN

Apache [Hadoop YARN](http://hadoop.apache.org/) is a cluster resource management framework. It allows to run various distributed applications on top of a cluster. Stratosphere runs on YARN next to other applications. So users do not have to setup or install anything if there is already a YARN setup.



<b>Requirements</b>
<ul>
    <li>Apache Hadoop 2.2</li>
    <li>HDFS</li>
</ul>


<section id="session">
### Start Stratosphere Session

Follow these instructions to learn how to lauch a Stratosphere Session within your YARN cluster. A session will start all required Stratosphere services (JobManager and TaskManagers) so that you can submit jobs to the cluster. Note that you can submit multiple jobs per session.

#### Get Stratosphere Uber-Jar

You only need one file to run Stratosphere on YARN, the <i>Stratosphere Uber-Jar</i>. Download the Uber-jar on the [download page]({{site.baseurl}}/downloads/#bin).


If you want to build the uber-jar from sources, follow the build instructions. Make sure to use the `-Dhadoop.profile=2` profile. You can find the Jar file in `stratosphere-dist/target/stratosphere-dist-{{site.current_snapshot}}-yarn-uberjar.jar` (*Note: The version might be different for you* ).

### Deploy

Invoke the Jar file using the following command:

```bash
java -jar stratosphere-dist-{{site.current_stable}}-yarn-uberjar.jar
```

This command will show you the following overview:

```bash
java -jar stratosphere-dist-{{site.current_stable}}-yarn-uberjar.jar 
Usage:
   Required
     -n,--container <arg>   Number of Yarn container to allocate (=Number of TaskTrackers)
   Optional
     -c,--conf <arg>                 Path to Stratosphere configuration file
     -g,--generateConf               Place default configuration file in current directory
     -j,--jar <arg>                  Path to Stratosphere jar file
     -jm,--jobManagerMemory <arg>    Memory for JobManager Container [in MB]
     -q,--query                      Display avilable YARN resources (memory, cores)
     -qu,--queue <arg>               Specify YARN queue.
     -tm,--taskManagerMemory <arg>   Memory per TaskManager Container [in MB]
     -tmc,--taskManagerCores <arg>   Virtual CPU cores per TaskManager
     -v,--verbose                    Verbose debug mode
```

Please note that the Client requires the `HADOOP_HOME` (or `YARN_CONF_DIR` or `HADOOP_CONF_DIR`) environment variable to be set to read the YARN and HDFS configuration.

**Example:** Issue the following command to allocate 10 TaskTrackers, with 8GB of memory each:

```bash
java -jar stratosphere-dist-{{site.current_stable}}-yarn-uberjar.jar -n 10 -tm 8192
```


The jar will automatically create a `stratosphere-config.yml` in the local directory and use this, if no config file has been specified. Please follow our [configuration guide]({{site.baseurl}}/docs/0.4/setup/config.html) if you want to change something. Stratosphere on YARN will overwrite the following configuration parameters `jobmanager.rpc.address` (because the JobManager is always allocated at different machines) and `taskmanager.tmp.dirs` (we are using the tmp directories given by YARN).

The example invocation starts 11 containers, since there is one additional container for the ApplicationMaster and JobTracker.

Once Stratosphere is deployed in your YARN cluster, it will show you the connection details of the JobTracker.

The client has to remain open to keep the deployment intact. We suggest to use `screen`. It will start another shell that is detachable.
So open `screen`, start Stratosphere on YARN, use `CTRL+a` then press `d` to detach the screen session. Use `screen -r` to resume again.
</section>

<section id="submission">
### Submit Job to Stratosphere


Use the following command to submit a Stratosphere job to the YARN cluster:

```
java -cp stratosphere-dist-{{site.current_stable}}-yarn-uberjar.jar eu.stratosphere.client.CliFrontend
```

If you have Stratosphere installed from a custom build or a zip file, use the [commandline client]({{site.baseurl}}/docs/0.4/program_execution/cli_client.html):

```
./bin/stratosphere
``` 

Both commands will show you a help like this:

```
[...]
Action "run" compiles and submits a Stratosphere program.
  "run" action arguments:
     -a,--arguments <programArgs>   Program arguments
     -c,--class <classname>         Program class
     -j,--jarfile <jarfile>         Stratosphere program JAR file
     -m,--jobmanager <host:port>    Jobmanager to which the program is submitted
     -w,--wait                      Wait for program to finish
[...]
```

Use the *run* action to submit a job to YARN. The uberjar will show you the address of the JobManager in the console.

**Example**

```bash
# Optionally download some sample data first
wget -O apache-license-v2.txt http://www.apache.org/licenses/LICENSE-2.0.txt
# Submit Job to Stratosphere
./bin/stratosphere run -m localhost:6123 \
                       -j ./examples/stratosphere-java-examples-{{site.current_stable}}-WordCount.jar \
                       -a 1 file://`pwd`/apache-license-v2.txt file://`pwd`/wordcount-result.txt 
```

You can use also script without the `-m` (or `--jobmanager`) argument, but you have to configure the `stratosphere-conf.yaml` with the correct JobManager details.
</section>

<section id="build">
### Build Stratosphere for a specific Hadoop version.

This section covers building Stratosphere for a specifiy Hadoop version. Most users do not need to do this manually.
The problem is that Stratosphere uses HDFS and YARN which are both from Apache Hadoop. There exist many different builds of Hadoop (from both the upstream project and the different Hadoop distributions.)
Typically errors arise with the RPC services. An error could look like this:

```
ERROR: The job was not successfully submitted to the nephele job manager: eu.stratosphere.nephele.executiongraph.GraphConversionException: Cannot compute input splits for TSV: java.io.IOException: Failed on local exception: com.google.protobuf.InvalidProtocolBufferException: Protocol message contained an invalid tag (zero).; Host Details :
```

**Example**

```
mvn -Dhadoop.profile=2 -Pcdh-repo -Dhadoop.version=2.0.0-cdh4.2.0 -P\!include-yarn -DskipTests package
```

The commands in detail:

*  `-Dhadoop.profile=2` activates the Hadoop YARN profile of Stratosphere. This will enable all components of Stratosphere that are compatible with Hadoop 2.2
*  `-Pcdh-repo` activates the Cloudera Hadoop dependencies. If you want other vendor's Hadoop dependencies (not in maven central) add the repository to your local maven configuration in `~/.m2/`.
* `-Dhadoop.version=2.0.0-cdh4.2.0` sets a special version of the Hadoop dependencies. Make sure that the specified Hadoop version is compatible with the profile you activated (non-YARN probably need `-Dhadoop.profile=1`)
* `-P!include-yarn` this command disables YARN in Stratosphere. This is required in this case because the Hadoop version we are using here `2.0.0-cdh4.2.0` is using the old YARN interface. As stated above, we expect Hadoop 2.2 (but Hadoop 2.1-betas might also work since they use the new APIs.)

Please post to the [Stratosphere mailinglist](https://groups.google.com/d/forum/stratosphere-users) or create an issue on [Github](https://github.com/stratosphere/stratosphere/issues), if you have issues with your YARN setup and Stratosphere.

</section>

<section id="background">
### Background

This section briefly describes how Stratosphere and YARN are working together. 
<img src="{{site.baseurl}}/img/StratosphereOnYarn.svg" class="img-responsive">

The YARN client needs to access the Hadoop configuration to connect to the YARN resource manager and to HDFS. The client determines the Hadoop configuration using the following strategy:

* Test if `YARN_CONF_DIR`, `HADOOP_CONF_DIR` or `HADOOP_CONF_PATH` are set (in that order). If one of these variables are set, they are used to read the configuration.
* If the above strategy failed (this should not be the case in a correct YARN setup.), the client is using the `HADOOP_HOME` environment variable. If it set, the client tries to access `$HADOOP_HOME/etc/hadoop` (Hadoop 2) and `$HADOOP_HOME/conf` (Hadoop 1).

When starting a new Stratosphere YARN session, the client first checks if the requested resources (containers and memory) are available. After that, it is uploading a jar that contains Stratosphere and the configuration to HDFS (step 1).

The next step of the client is to request (step 2) a YARN container to start the *ApplicationMaster* (step 3). Since the client registered the configuration and jar-file as a resource for the container, the NodeManager of YARN running on that particular machine will take care of preparing the container (e.g. downloading the files). Once that has finished, the *ApplicationMaster* (AM) is started.

The *JobManager* and AM are running in the same container. Once they successfully started, the AM knows the address of the JobManager (its own host). It is generating a new Stratosphere configuration file for the TaskManagers (so that they can connect to the JobManager). The file is also uploaded into HDFS. Additionally, the *AM* container is also serving Stratosphere's web interface.

After that, the AM starts allocating the containers for the TaskManagers for Stratosphere. They will also download the jar file and the modified configuration from the HDFS. Once these steps completed, Stratosphere is set up for accepting Jobs.


</section>
