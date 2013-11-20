#EAP WS subsystem administration tests#

mvn clean verify -Dit.test=CLIWebservicesModifyWsdlAddressIT#testChangeFollowedByReloadAffectsNewDeployments -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR2/build/jboss-eap-6.2

mvn clean verify -Dit.test=CLIWebservicesModifyWsdlAddressIT#testWsdlIsAccessibleAfterReload -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR2/build/jboss-eap-6.2 -DreloadWaitMillis=4000 -DtestWsdlIsAccessibleAfterReload.reloadCount=6 2>&1 | tee log.txt

fails (reload wait 4s)

    mvn clean verify -Dit.test=CLIWebservicesWsdlIT#testWsdlIsAccessibleAfterReload -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR2/build/jboss-eap-6.2 -DreloadWaitMillis=4000 -DtestWsdlIsAccessibleAfterReload.reloadCount=6 2>&1 | tee log.txt
passes (reload wait 5s)

    mvn clean verify -Dit.test=CLIWebservicesWsdlIT#testWsdlIsAccessibleAfterReload -Djboss.home=/home/development/jbossqe/JBEAP-6.2.0.CR2/build/jboss-eap-6.2 -DreloadWaitMillis=5000 -DtestWsdlIsAccessibleAfterReload.reloadCount=6 2>&1 | tee log.txt
