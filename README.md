#EAP WS subsystem administration tests#

mvn -Dtest=CLIWebservicesModifyWsdlAddressIT#testChangeFollowedByReloadAffectsNewDeployments -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.ER7/build/jboss-eap-6.2 test
