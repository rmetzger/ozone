mvn verify # assuming that hadoop_v1 profile is activated by default
if [ $? -ne 0 ]; then
	echo "mvn verify failed for hadoop_v1 maven profile. Aborting build!"
	return $?
fi
echo "mvn verify for hadoop_v1 was successful. Verifying hadoop yarn"
mvn -Phadoop_yarn clean verify
return $?
