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
