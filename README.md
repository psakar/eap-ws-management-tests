#EAP WS subsystem administration tests#

To run

    mvn clean verify -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2

To run single test

    mvn clean verify -Dit.test=CLIWebservicesModifyWsdlAddressIT#testChangeFollowedByReloadAffectsNewDeployments -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2

To change server reload wait time 

    mvn clean verify -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2 -DreloadWaitMillis=4000 

Redirect log to file example

    mvn clean verify -Dit.test=CLIWebservicesModifyWsdlAddressIT#testWsdlIsAccessibleAfterReload -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2 -DreloadWaitMillis=4000 2>&1 | tee log.txt

To change test timeout

	-Dtest.timeoutMillis=60000
	
To test if WS is functional
	
	-DtestIfServiceIsFunctional=1
	
	
To see failures related to connection pooling run

    mvn clean verify -Dit.test=CLIWebservicesWsdlIT -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2 -DreloadWaitMillis=1000 -DdisconnectURLconnection=0 -DretryReadFromURLconnection=0 2>&1 | tee log.txt
    
To see how failures related to connection pooling are solved by setting system property "http.keepAlive" to false

    mvn clean verify -Dit.test=CLIWebservicesWsdlIT -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2 -DreloadWaitMillis=1000 -DtestIfServiceIsFunctional=0 -DdisconnectURLconnection=0 -Dhttp.keepAlive=false 2>&1 | tee log.txt    

To see failures related to connection pooling are solved by closing connection, not by closing stream

    mvn clean verify -Dit.test=CLIWebservicesWsdlIT -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2 -DreloadWaitMillis=1000 -DtestIfServiceIsFunctional=0 -DdisconnectURLconnection=0 -DcloseURLstream=1 -DretryReadFromURLconnection=0 2>&1 | tee log.txt

To see how failures related to connection pooling are solved by setting system property "http.keepAlive" to false even neither stream or connection are closed

    mvn clean verify -Dit.test=CLIWebservicesWsdlIT -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/build/jboss-eap-6.2 -DreloadWaitMillis=1000 -DtestIfServiceIsFunctional=0 -DdisconnectURLconnection=0 -DcloseURLstream=0 -Dhttp.keepAlive=false 2>&1 | tee log.txt    

To see failures related to connection pooling caused by creating web service from WSDL

    mvn clean verify -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR3/jboss-eap-6.2-fixed -DretryReadFromURLconnection=0 2>&1 | tee log.txt
