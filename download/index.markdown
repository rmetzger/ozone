--- 
layout: inner_simple
title: Download
---

<p style="font-size: 50px;margin-bottom:50px" class="text-center">Download</p>

<p class="text-center">There are plenty of ways to get Stratosphere. Texts on the left column explain the details</p>

<table class="table table-bordered table-striped">
	<thead>
		<tr>
			<th style="width:300px" ><p style="font-weight:normal;">Stratosphere has some dependencies to Hadoop for example for HDFS or HBase.
			Choose a Stratosphere distribution that matches your Hadoop installation's version. In doubt, use the Stratosphere version for Hadoop 1.2.X.</p>
			</th>
			<th class="text-center"><b>Hadoop 1.2.X</b>
			</th>
			<th class="text-center">Hadoop 2 (YARN)
				<p style="font-weight:normal;">Includes support for HBase and runs on YARN</p>
			</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td style="width:300px"><b>Binary</b> <br>
				Download as a binary if you want to install and run Stratosphere on a computer or cluster.<br>
				<p><a href="/docs/gettingstarted.html">Installation Guide</a></p>
			</td>
			<td class="text-center"><a class="btn btn-primary btn-default" target="_blanc" href="http://dopa.dima.tu-berlin.de/bin/stratosphere-0.4-SNAPSHOT.tgz">
      <i class="icon-download"> </i>Download</a>
			
			</td>
			<td class="text-center"><a class="btn btn-primary btn-default" target="_blanc" href="http://dopa.dima.tu-berlin.de/bin/stratosphere-0.4-hadoop2-SNAPSHOT.tgz">
      <i class="icon-download"> </i>Download</a>
			</td>
		</tr>

		<tr>
			<td><b>Quickstart</b><br>
			You do not need to setup Stratosphere in order to start writing Jobs. These quickstart one-liners will create a directory structure conaining everything you need to start with Stratosphere.
			<p>For more information see <a href="/quickstart/">Quickstart</a></p>
		</td>
			<td  colspan="2">
				Java: <pre class="prettyprint" style="padding-left:1em">curl https://raw.github.com/stratosphere/stratosphere-quickstart/master/quickstart.sh | bash</pre>

				Scala: <pre class="prettyprint" style="padding-left:1em">curl https://raw.github.com/stratosphere/stratosphere-quickstart/master/quickstart-scala.sh | bash</pre>
				<b>Click here to see the command being executed</b>
			</td>
		</tr>


		<tr>
			<td style="width:300px"><b>Maven Dependencies</b><br /><br />
				Use the Maven Dependencies to add Stratosphere to your project, or if you do not want to use the Quickstart scripts.
			</td>
			<td>
			<pre class="prettyprint" style="padding-left:1em">
				
&lt;project&gt;
  ...
  &lt;dependencies&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;eu.stratosphere&lt;/groupId&gt;
      &lt;artifactId&gt;stratosphere&lt;/artifactId&gt;
      &lt;version&gt;0.4-SNAPSHOT&lt;/version&gt;
      &lt;type&gt;pom&lt;/version&gt;
    &lt;/dependency&gt;
  &lt;/dependencies&gt;
&lt;/project&gt;
</pre>
<b>Note by robert: These dependencies do not work!</b>
			</td>
			<td><pre class="prettyprint" style="padding-left:1em">
&lt;project&gt;
  ...
  &lt;dependencies&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;eu.stratosphere&lt;/groupId&gt;
      &lt;artifactId&gt;stratosphere&lt;/artifactId&gt;
      &lt;version&gt;0.4-hadoop2-SNAPSHOT&lt;/version&gt;
      &lt;type&gt;pom&lt;/version&gt;
    &lt;/dependency&gt;
  &lt;/dependencies&gt;
&lt;/project&gt;
</pre>
			</td>
		</tr>

		<tr>
			<td><b>Virtual Machine</b><br>
				<p>If you want to try out Stratosphere, download our Virtual Machine Images. They run with Virtual Box and VMWare.</a></p>
			</td>
			<td class="text-center" colspan="2">
				<a class="btn btn-primary btn-lg" target="_blanc" href="http://dev.stratosphere.eu/vm/">
      				<i class="icon-download"> </i>Download</a>
				
			</td>
		</tr>
		<tr>
			<td><b>Vagrant</b></td>
			<td class="text-center" colspan="2">
				<pre class="prettyprint" style="padding-left:1em; text-align: left; max-width: 400px; margin: 0 auto">
wget http://dev.stratosphere.eu/vm/Vagrantfile
vagrant up
vagrant ssh</pre></p>
			</td>
		</tr>

		<tr>
			<td><b>Debian Package</b></td>
			<td colspan="2">
				<pre class="prettyprint" style="padding-left:1em">
# vim /etc/apt/sources.list.d/stratosphere.list
deb http://dev.stratosphere.eu/repo/binary precise main</pre>
				<pre class="prettyprint" style="padding-left:1em">
# apt-get update
apt-get install stratosphere-dist</pre>
				<p style="text-align: right; font-style: italic;">For more information see <a href="/quickstart/">Quickstart</a></p>
			</td>
		</tr>

		<tr>
			<td><b>Source</b><br /><br />
			Compile stratosphere on your own machine.<br>
			<a href="{{ site.baseurl }}/build.html">Build Instructions</a>
		</td>
			<td class="text-center">
				<pre class="prettyprint" style="padding-left:1em; text-align: left">
git clone https://github.com/stratosphere/stratosphere.git
cd stratosphere
mvn clean package -DskipTests</pre>
			</td>
			<td class="text-center">
				<pre class="prettyprint" style="padding-left:1em; text-align: left">
git clone https://github.com/stratosphere/stratosphere.git
cd stratosphere
mvn clean package -DskipTests -Dhadoop.profile=2</pre>
			</td>
		</tr>
	</tbody>
</table>


<!--
<div class="bs-docs-grid">
<div class="row">
	<div class="col-md-2"></div>
	<div class="col-md-5 text-center"><h3>Hadoop 1.2.X</h3></div>
	<div class="col-md-5text-center"><h3>Hadoop 2 (YARN)</h3></div>
</div>
<div class="row">
	<div class="col-md-2"><b>Binary</b></div>
	<div class="col-md-5 text-center">
	<a class="btn btn-primary btn-lg" target="_blanc" href="http://dopa.dima.tu-berlin.de/bin/stratosphere-0.4-SNAPSHOT.tgz"><i class="icon-download"> </i>Download</a>
	<p style="text-align: right; font-style: italic;"><a href="/docs/gettingstarted.html">How to install?</a></p>
	</div>
	<div class="col-md-5 text-center">
	<a class="btn btn-primary btn-lg" target="_blanc" href="http://dopa.dima.tu-berlin.de/bin/stratosphere-0.4-hadoop2-SNAPSHOT.tgz"><i class="icon-download"> </i>Download</a>
	<p style="text-align: right; font-style: italic;"><a href="/docs/gettingstarted.html">How to install?</a></p>
	</div>
</div>
<div class="row">
	<div class="col-md-2"><b>Maven Dependencies</b><p>adding stratosphere to your own maven project</div>
	<div class="col-md-5">
	<pre class="prettyprint" style="padding-left:1em">
&lt;project&gt;
  ...
  &lt;dependencies&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;eu.stratosphere&lt;/groupId&gt;
      &lt;artifactId&gt;todo&lt;/artifactId&gt;
      &lt;version&gt;todo&lt;/version&gt;
      &lt;type&gt;bar&lt;/type&gt;
      &lt;scope&gt;runtime&lt;/scope&gt;
    &lt;/dependency&gt;
  &lt;/dependencies&gt;
&lt;/project&gt;
	</pre>
	</div>
	<div class="col-md-5">
	<pre class="prettyprint" style="padding-left:1em">
&lt;project&gt;
  ...
  &lt;dependencies&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;eu.stratosphere&lt;/groupId&gt;
      &lt;artifactId&gt;todo&lt;/artifactId&gt;
      &lt;version&gt;todo&lt;/version&gt;
      &lt;type&gt;bar&lt;/type&gt;
      &lt;scope&gt;runtime&lt;/scope&gt;
    &lt;/dependency&gt;
  &lt;/dependencies&gt;
&lt;/project&gt;
	</pre>
	</div>
</div>
<div class="row">
	<div class="col-md-2"><b>Quickstart</b></div>
	<div class="col-md-10 text-center">
		Java: <pre class="prettyprint" style="padding-left:1em">curl https://raw.github.com/stratosphere/stratosphere-quickstart/master/quickstart.sh | bash</pre>
		Scala: <pre class="prettyprint" style="padding-left:1em">curl https://raw.github.com/stratosphere/stratosphere-quickstart/master/quickstart.sh | bash</pre>
		<p style="text-align: right; font-style: italic;">For more information see <a href="/quickstart/">Quickstart</a></p>
	</div>
</div>
</div>
-->
